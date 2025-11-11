package net.pascalhp.webprobe.checker.checkers.gethostbyname;

import net.pascalhp.webprobe.checker.CheckResult;

public class GethostbynameCheckResult extends CheckResult {
    public boolean isIPv4;
    public boolean isIPv6;
    public String ip;
    public Throwable error;

    public GethostbynameCheckResult(String reportPageTitle) {
        super(reportPageTitle);
    }
}
