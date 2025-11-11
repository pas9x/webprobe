package net.pascalhp.webprobe.checker.checkers.http;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.checker.Checker;

import java.net.URL;

import static net.pascalhp.webprobe.Localization.lang;

public class HttpChecker implements Checker {
    private final URL url;
    private HttpTrace trace;
    private String stopReason;

    public HttpChecker(URL url) {
        this.url = url;
    }

    public HttpCheckResult check() {
        HttpCheckResult result = new HttpCheckResult(this.getName());
        if (this.checkSecure(result)) {
            result.reportPage.title = this.getName();
            return result;
        }
        this.checkInsecure(result);
        result.reportPage.title = this.getName();
        return result;
    }

    private boolean checkSecure(HttpCheckResult result) {
        HttpTrace trace = new HttpTrace(this.url, false);
        this.trace = trace;
        result.secureCheckResult = new HttpCheckResult.URLCheckResult();
        result.reportPage = this.trace.trace();
        result.success = this.trace.success;
        result.secureCheckResult.reportPage = result.reportPage;
        result.secureCheckResult.tcp = this.trace.tcp;
        result.secureCheckResult.tls = this.trace.tls;
        result.secureCheckResult.http = this.trace.http;
        this.trace = null;
        if (result.success) {
            return true;
        }
        if (!trace.isHttps) {
            return true;
        }
        if (!trace.tcp.isSuccess) {
            return true;
        }
        if (trace.tls.isSuccess) {
            return true;
        }
        if (this.stopReason != null) {
            return true;
        }
        return false;
    }

    private void checkInsecure(HttpCheckResult result) {
        this.trace = new HttpTrace(this.url, true);
        result.insecureCheckResult = new HttpCheckResult.URLCheckResult();
        result.reportPage.log(lang("check.http.try_insecure"));
        result.insecureCheckResult.reportPage = this.trace.trace();
        result.secureCheckResult.tcp = this.trace.tcp;
        result.secureCheckResult.tls = this.trace.tls;
        result.secureCheckResult.http = this.trace.http;
        result.reportPage.append(result.insecureCheckResult.reportPage);
        this.trace = null;
    }

    public void stop(String reason) {
        this.stopReason = reason;
        if (this.trace != null) {
            try {
                this.trace.stop(reason);
            } catch (Throwable e) {
                ErrorHandler.logException(e);
            }
        }
    }

    public String getName() {
        return "HTTP";
    }
}
