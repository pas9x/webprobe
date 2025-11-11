package net.pascalhp.webprobe.exceptions;

public class BugException extends RuntimeException {
    public BugException(String msg) {
        super("BUG: " + msg);
    }

    public BugException(String msg, Throwable cause) {
        super("BUG: " + msg, cause);
    }
}
