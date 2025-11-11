package net.pascalhp.webprobe.checker;

public interface Checker {
    public String getName();
    public CheckResult check();
    public void stop(String reason);
}
