package net.pascalhp.webprobe.checker.checkers.dnsjava;

import java.util.List;

public class LookupResult {
    public final boolean success;
    public final List<String> records;
    public final Throwable error;

    public LookupResult(boolean success, List<String> records, Throwable error) {
        this.success = success;
        this.records = records;
        this.error = error;
    }
}
