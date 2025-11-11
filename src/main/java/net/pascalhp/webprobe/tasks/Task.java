package net.pascalhp.webprobe.tasks;

public interface Task {
    public Object run();
    public void stop(String reason);
}
