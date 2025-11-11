package net.pascalhp.webprobe.checker.checkers.dnsjava;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;
import net.pascalhp.webprobe.exceptions.BugException;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

public class DnsjavaChecker implements Checker {
    public final String domain;
    public final List<String> resolvers;
    protected String stopReason;

    public DnsjavaChecker(String domain, List<String> resolvers) {
        this.domain = domain;
        this.resolvers = resolvers;
        if (resolvers == null || resolvers.isEmpty()) {
            throw new IllegalArgumentException("Empty resolvers list");
        }
    }

    public CheckResult check() {
        DnsjavaCheckResult result = new DnsjavaCheckResult(this.getName());

        boolean haveAnyRecord = false;

        for (String resolverHost : this.resolvers) {
            if (this.stopReason != null) {
                result.reportPage.log(lang("check.stop_reason", Map.of("reason", this.stopReason)));
                break;
            }

            result.reportPage.log(lang("check.dnsjava.start", Map.of("resolver", resolverHost)));
            LookupResult lookupResult;

            try {
                List<String> records = this.getRecords(resolverHost, this.domain, "ANY");
                lookupResult = new LookupResult(true, records, null);
                if (!records.isEmpty()) {
                    haveAnyRecord = true;
                }
                result.reportPage.log(lang("check.dnsjava.records", Map.of("records", String.join("\n", records))));
            } catch (Throwable e) {
                result.success = false;
                lookupResult = new LookupResult(true, null, e);
                result.reportPage.log(lang("check.dnsjava.fail", Map.of("error", ErrorHandler.formatException(e))));
            }

            result.lookups.put(resolverHost, lookupResult);
        }

        if (result.success == null && haveAnyRecord) {
            result.success = true;
        }

        return result;
    }

    public void stop(String reason) {
        this.stopReason = reason;
    }

    protected List<String> getRecords(String resolverHost, String domain, String type) throws Throwable {
        SimpleResolver resolver = new SimpleResolver(resolverHost);
        Lookup lookup = new Lookup(domain, Type.value(type));
        lookup.setResolver(resolver);
        Record[] records = lookup.run();

        if (lookup.getResult() != Lookup.SUCCESSFUL) {
            throw new RuntimeException("DNS request failed: " + lookup.getErrorString());
        }

        if (records == null) {
            throw new BugException("Dnsjava didn't returned records list");
        }

        LinkedList<String> result = new LinkedList<>();
        for (Record record : records) {
            result.add(record.toString());
        }

        return result;
    }

    public String getName() {
        return "Dnsjava";
    }
}
