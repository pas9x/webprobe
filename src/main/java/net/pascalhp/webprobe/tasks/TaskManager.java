package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.Event;
import net.pascalhp.webprobe.EventRouter;
import net.pascalhp.webprobe.WaitGroup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TaskManager {
    public final EventRouter events;
    protected String stopReason = null;

    protected boolean started = false;
    protected boolean exited = false;

    protected LinkedList<TaskThread> taskThreads = new LinkedList<>();
    protected HashMap<String, TaskResult> taskResults = new HashMap<>();
    protected WaitGroup wgInternal = new WaitGroup();
    protected WaitGroup wgExternal = new WaitGroup();
    protected Thread internalWaitExitThread;

    public TaskManager(EventRouter events) {
        this.events = events;
        this.events.addEventListener(this::onEvent);
    }

    public TaskManager() {
        this(new EventRouter());
    }

    public void start() {
        synchronized (this) {
            if (this.started) {
                throw new RuntimeException("Unable to start task manager twice");
            }
            this.started = true;
            this.wgExternal.enter();
            this.events.pushEvent(new ManagerStartEvent());

            for (TaskThread thread : this.taskThreads) {
                this.startThread(thread);
            }

            this.internalWaitExitThread = new Thread(this::internalWaitAllExit);
            this.internalWaitExitThread.start();
        }
    }

    public boolean stop(String reason) {
        synchronized (this) {
            if (!this.started || this.exited || this.stopReason != null) {
                return false;
            }
            if (reason == null || reason.isEmpty()) {
                throw new RuntimeException("Task manager stop reason should not be empty");
            }

            for (TaskThread thread : this.taskThreads) {
                try {
                    thread.stop(reason);
                } catch (Throwable e) {
                    ErrorHandler.logException(e);
                }
            }

            this.stopReason = reason;

            return true;
        }
    }

    public String getStopReason() {
        return this.stopReason;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean isExited() {
        return this.exited;
    }

    public boolean addTask(String id, Task task) {
        return addTask(id, task, 0);
    }

    // TODO: forbid task with same ids
    public boolean addTask(String id, Task task, int timeout) {
        synchronized (this) {
            if (id == null) {
                throw new NullPointerException("Task id == null");
            }
            if (this.exited || this.stopReason != null) {
                return false;
            }
            TaskThread thread = new TaskThread(id, task, this.wgInternal, this.events, timeout);
            this.taskThreads.add(thread);
            if (this.started) {
                this.startThread(thread);
            }
            return true;
        }
    }

    protected void startThread(TaskThread thread) {
        this.wgInternal.enter();
        if (thread.timeout > 0) {
            thread.timeoutThread = new Thread(thread::waitTimeout);
            thread.timeoutThread.start();
        }
        thread.thread.start();
    }

    public void waitAllExit() {
        try {
            this.wgExternal.waitAllExit();
        } catch (InterruptedException e) {
            this.stop("TaskManager.waitAllExit(): thread has interrupted");
        }
    }

    protected void internalWaitAllExit() {
        try {
            this.wgInternal.waitAllExit();
        } catch (Throwable e) {
            this.stop("TaskManager.internalWaitAllExit(): thread has interrupted");
        }
        this.exited = true;
        this.events.pushEvent(new ManagerExitEvent(this.stopReason));
        this.wgExternal.exit();
    }

    public Map<String, TaskResult> getResults() {
        return this.exited ? this.taskResults : null;
    }

    protected void onEvent(Event event) {
        if (event instanceof TaskExitEvent) {
            TaskExitEvent exitEvent = (TaskExitEvent)event;
            this.taskResults.put(exitEvent.id, exitEvent.result);
        }
    }
}
