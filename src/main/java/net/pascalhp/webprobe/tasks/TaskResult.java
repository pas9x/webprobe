package net.pascalhp.webprobe.tasks;

public class TaskResult {
    public final boolean isStopped;
    public final String stopReason;
    public final boolean isError;
    public final Throwable error;
    public final Object result;

    public TaskResult(String stopReason, Throwable error, Object result) {
        this.isStopped = (stopReason != null);
        this.stopReason = stopReason;
        this.isError = (error != null);
        this.error = error;
        this.result = result;
    }
}
