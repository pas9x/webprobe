package net.pascalhp.webprobe.checker.checkers.mtr;

import net.pascalhp.webprobe.OSType;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;

public class MtrChecker implements Checker {
    public final OSType os;
    public final String basedir;
    public final String host;
    public final int count;
    protected final Checker realChecker;

    public MtrChecker(OSType os, String basedir, String host, int count) {
        this.os = os;
        this.basedir = basedir;
        this.host = host;
        this.count = count;

        if (count < 1) {
            throw new IllegalArgumentException("Mtr pings count must not be less than 1");
        }

        if (os == OSType.UNIX) {
            this.realChecker = new MtrCheckerUnix(host, count);
        } else if(os == OSType.WINDOWS) {
            this.realChecker = new MtrCheckerWindows(basedir, host, count);
        } else if (os == OSType.MACOS) {
            this.realChecker = new MtrCheckerMacos(basedir, host, count);
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
        return "MTR";
    }
}
