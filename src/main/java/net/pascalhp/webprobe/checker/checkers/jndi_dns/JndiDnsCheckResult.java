package net.pascalhp.webprobe.checker.checkers.jndi_dns;

import net.pascalhp.webprobe.checker.CheckResult;

import java.util.List;

public class JndiDnsCheckResult extends CheckResult {
    public List<String> aRecords;
    public List<String> aaaaRecords;
    public Throwable aError;
    public Throwable aaaaError;

    public JndiDnsCheckResult(String reportPageTitle) {
        super(reportPageTitle);
    }
}
