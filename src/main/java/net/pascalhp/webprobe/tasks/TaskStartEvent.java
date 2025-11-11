package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.Event;

public class TaskStartEvent implements Event {
    public final String id;
    public final Task task;

    public TaskStartEvent(String id, Task task) {
        this.id = id;
        this.task = task;
    }
}
