package net.pascalhp.webprobe.checker.checkers.mtr;

import net.pascalhp.webprobe.checker.CheckResult;

public class MtrCheckResult extends CheckResult {
    public int exitCode = -2;
    public String mtrOutput;
    public Throwable error;

    public MtrCheckResult(String reportPageTitle) {
        super(reportPageTitle);
    }
}
