package net.pascalhp.webprobe.tasks;

import net.pascalhp.webprobe.Event;

public class ManagerExitEvent implements Event {
    public final String stopReason;

    public ManagerExitEvent(String stopReason) {
        this.stopReason = stopReason;
    }
}
