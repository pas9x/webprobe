package net.pascalhp.webprobe.checker.checkers.dnsjava;

import net.pascalhp.webprobe.checker.CheckResult;

import java.util.HashMap;
import java.util.Map;

public class DnsjavaCheckResult extends CheckResult {
    public Map<String, LookupResult> lookups = new HashMap<>();

    public DnsjavaCheckResult(String reportPageTitle) {
        super(reportPageTitle);
    }
}
