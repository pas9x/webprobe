package net.pascalhp.webprobe.checker.checkers.http;

import net.pascalhp.webprobe.Report;
import net.pascalhp.webprobe.SSLCertificate;
import net.pascalhp.webprobe.checker.CheckResult;

import java.util.List;

public class HttpCheckResult extends CheckResult {
    public URLCheckResult secureCheckResult;
    public URLCheckResult insecureCheckResult;

    public HttpCheckResult(String reportPageTitle) {
        super(reportPageTitle);
    }

    public static class TCPLayer {
        public boolean isSuccess;
        public String host;
        public int port;
        public long dnsTimeMs;
        public long connectTimeMs;
        public Throwable error;
    }

    public static class TLSLayer {
        public boolean isSuccess;
        public List<SSLCertificate> serverCertificates;
        public String sslVersion;
        public String sslCipher;
        public long handshakeTimeMs;
        public Throwable error;
    }

    public static class HTTPLayer {
        public boolean isSuccess;
        public Request request;
        public Response response;
        public Throwable error;
    }

    public static class URLCheckResult {
        public boolean isSuccess;
        public Report.Page reportPage;
        public TCPLayer tcp;
        public TLSLayer tls;
        public HTTPLayer http;
    }
}
