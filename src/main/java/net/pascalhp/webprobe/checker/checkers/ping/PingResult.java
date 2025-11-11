package net.pascalhp.webprobe.checker.checkers.ping;

import net.pascalhp.webprobe.Report;
import net.pascalhp.webprobe.checker.CheckResult;

public class PingResult extends CheckResult {
    public int exitCode = -2;
    public String pingOutput;
    public Throwable error;

    public PingResult(String reportPageTitle) {
        super(reportPageTitle);
    }
}
