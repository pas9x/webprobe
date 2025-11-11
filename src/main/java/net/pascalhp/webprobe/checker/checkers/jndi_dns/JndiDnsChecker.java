package net.pascalhp.webprobe.checker.checkers.jndi_dns;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

public class JndiDnsChecker implements Checker {
    public final String domain;
    protected String stopReason;

    public JndiDnsChecker(String domain) {
        this.domain = domain;
    }

    public CheckResult check() {
        JndiDnsCheckResult result = new JndiDnsCheckResult(this.getName());

        boolean haveArecords = false;
        boolean haveAAAArecords = false;

        if (this.stopped(result)) {
            return result;
        }

        try {
            result.reportPage.log(lang("check.jndi_dns.start_a", Map.of("domain", this.domain)));
            result.aRecords = this.getRecords(this.domain, "A");
            if (result.aRecords.isEmpty()) {
                result.reportPage.log(lang("check.jndi_dns.no_a_records"));
            } else {
                haveArecords = true;
                result.reportPage.log(lang("check.jndi_dns.a_records", Map.of("records", String.join("\n", result.aRecords))));
            }
        } catch (NamingException e) {
            result.success = false;
            result.aError = e;
            result.reportPage.log(lang("check.jndi_dns.error", Map.of("error", e.toString())));
        } catch (Throwable e) {
            result.success = false;
            result.aError = e;
            result.reportPage.log(lang("check.jndi_dns.error", Map.of("error", ErrorHandler.formatException(e))));
        }

        if (this.stopped(result)) {
            return result;
        }

        try {
            result.reportPage.log(lang("check.jndi_dns.start_aaaa", Map.of("domain", this.domain)));
            result.aaaaRecords = this.getRecords(this.domain, "AAAA");
            if (result.aaaaRecords.isEmpty()) {
                result.reportPage.log(lang("check.jndi_dns.no_aaaa_records"));
            } else {
                haveAAAArecords = true;
                result.reportPage.log(lang("check.jndi_dns.aaaa_records", Map.of("records", String.join("\n", result.aaaaRecords))));
            }
        } catch (NamingException e) {
            result.success = false;
            result.aaaaError = e; // Error for A record is more important
            result.reportPage.log(lang("check.jndi_dns.error", Map.of("error", e.toString())));
        } catch (Throwable e) {
            result.success = false;
            result.aaaaError = e;
            result.reportPage.log(lang("check.jndi_dns.error", Map.of("error", ErrorHandler.formatException(e))));
        }

        if (result.success == null) {
            if (haveArecords || haveAAAArecords) {
                result.success = true;
            }
        }

        return result;
    }

    protected boolean stopped(CheckResult result) {
        if (this.stopReason == null) {
            return false;
        } else {
            result.success = false;
            result.reportPage.log(lang("check.stop_reason", Map.of("reason", this.stopReason)));
            return true;
        }
    }

    public void stop(String reason) {
        this.stopReason = reason;
    }

    protected List<String> getRecords(String domain, String type) throws NamingException {
        List<String> result = new ArrayList<>();
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        DirContext dirContext = new InitialDirContext(env);
        Attributes attrs = dirContext.getAttributes(domain, new String[] {type});
        Attribute attr = attrs.get(type);

        if (attr != null) {
            for (int i = 0; i < attr.size(); i++) {
                result.add(attr.get(i).toString());
            }
        }

        return result;
    }

    public String getName() {
        return "JNDI DNS";
    }
}
