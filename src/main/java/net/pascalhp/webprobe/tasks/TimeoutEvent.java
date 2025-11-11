package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.Event;

public class TimeoutEvent implements Event {
    public final String id;
    public final Task task;
    public final int timeout;

    public TimeoutEvent(String id, Task task, int timeout) {
        this.id = id;
        this.task = task;
        this.timeout = timeout;
    }
}
