package net.pascalhp.webprobe.checker.events;

import net.pascalhp.webprobe.Event;
import net.pascalhp.webprobe.checker.CheckResult;

public class OnCheckExit implements Event {
    public final String taskId;
    public final CheckResult result;
    public final Throwable error;

    public OnCheckExit(String taskId, CheckResult result, Throwable error) {
        this.taskId = taskId;
        this.result = result;
        this.error = error;
    }

    public String toString() {
        String result = "Exit check #" + this.taskId;
        if (this.error != null) {
            result += " (" + error.toString() + ")";
        }
        return result;
    }
}
