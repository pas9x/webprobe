package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.Event;

public class TaskExitEvent implements Event {
    public final String id;
    public final Task task;
    public final TaskResult result;

    public TaskExitEvent(String id, Task task, TaskResult result) {
        this.id = id;
        this.task = task;
        this.result = result;
    }
}
