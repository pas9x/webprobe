package net.pascalhp.webprobe.checker.events;

import net.pascalhp.webprobe.Event;
import net.pascalhp.webprobe.checker.Checker;

public class OnCheckStart implements Event {
    public final String taskId;
    public final Checker checker;

    public OnCheckStart(String taskId, Checker checker) {
        this.taskId = taskId;
        this.checker = checker;
    }

    public String toString() {
        return "Start check #" + this.taskId;
    }
}
