package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.EventRouter;
import net.pascalhp.webprobe.WaitGroup;

import java.util.concurrent.TimeUnit;

class TaskThread implements Runnable {
    public final String id;
    public final Task task;
    public final Thread thread;
    public final int timeout;
    protected final WaitGroup wg;
    protected final EventRouter events;
    protected String stopReason = null;
    protected Thread timeoutThread;

    public TaskThread(String id, Task task, WaitGroup wg, EventRouter events, int timeout) {
        this.id = id;
        this.task = task;
        this.wg = wg;
        this.events = events;
        this.thread = new Thread(this);
        this.timeout = timeout;
    }

    public void run() {
        TaskStartEvent startEvent = new TaskStartEvent(this.id, this.task);
        this.events.pushEvent(startEvent);

        Object taskResult = null;
        Throwable taskError = null;
        try {
            taskResult = task.run();
        } catch (Throwable e) {
            taskError = e;
        }
        wg.exit();

        TaskResult result = new TaskResult(this.stopReason, taskError, taskResult);
        TaskExitEvent exitEvent = new TaskExitEvent(this.id, this.task, result);
        this.events.pushEvent(exitEvent);

        if (this.timeoutThread != null) try {
            this.timeoutThread.interrupt();
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }
    }

    public void stop(String reason) {
        this.stopReason = reason;
        this.task.stop(reason);
    }

    public void waitTimeout() {
        try {
            TimeUnit.SECONDS.sleep(this.timeout);
            this.events.pushEvent(new TimeoutEvent(this.id, this.task, this.timeout));
            this.stop("Timeout " + this.timeout + "sec");
        } catch (InterruptedException e) {
        } catch (Throwable e) {
            ErrorHandler.logException(e);
        }
        this.timeoutThread = null;
    }
}
