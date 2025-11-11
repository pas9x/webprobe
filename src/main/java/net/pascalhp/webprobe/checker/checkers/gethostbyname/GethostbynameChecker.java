package net.pascalhp.webprobe.checker.checkers.gethostbyname;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.CheckResult;
import net.pascalhp.webprobe.checker.Checker;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

public class GethostbynameChecker implements Checker {
    public final String host;

    public GethostbynameChecker(String host) {
        this.host = host;
    }

    public CheckResult check() {
        GethostbynameCheckResult result = new GethostbynameCheckResult(this.getName());
        try {
            InetAddress addr = InetAddress.getByName(this.host);
            result.ip = addr.getHostAddress();
            if (addr instanceof Inet4Address) {
                result.reportPage.log(lang("check.gethostbyname.ipv4", Map.of("ip", result.ip)));
                result.isIPv4 = true;
                result.isIPv6 = false;
            } else if (addr instanceof Inet6Address) {
                result.reportPage.log(lang("check.gethostbyname.ipv6", Map.of("ip", result.ip)));
                result.isIPv4 = false;
                result.isIPv6 = true;
            }
            result.success = true;
        } catch (UnknownHostException e) {
            result.reportPage.log(lang("check.gethostbyname.error", Map.of("error", e.toString())));
        } catch (Throwable e) {
            result.reportPage.log(lang("check.gethostbyname.error", Map.of("error", ErrorHandler.formatException(e))));
        }

        return result;
    }

    public void stop(String reason) {
    }

    public String getName() {
        return "Gethostbyname";
    }
}
