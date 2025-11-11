package net.pascalhp.webprobe.checker.checkers.ping;

import net.pascalhp.webprobe.OSType;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;

public class PingChecker implements Checker {
    public final OSType os;
    public final String host;
    public final int count;
    protected final Checker realChecker;

    public PingChecker(OSType os, String host, int count) {
        this.os = os;
        this.host = host;
        this.count = count;

        if (count < 1) {
            throw new IllegalArgumentException("Ping count must not be less than 1");
        }

        if (os == OSType.UNIX || os == OSType.MACOS) {
            this.realChecker = new PingCheckerUnix(host, count);
        } else if (os == OSType.WINDOWS) {
            this.realChecker = new PingCheckerWindows(host, count);
        } else if (os == null) {
            throw new NullPointerException("os == null");
        } else {
            throw new IllegalArgumentException("Unsupported os: " + os.toString());
        }
    }

    public CheckResult check() {
        return this.realChecker.check();
    }

    public void stop(String reason) {
        this.realChecker.stop(reason);
    }

    public String getName() {
        return "Ping";
    }
}
