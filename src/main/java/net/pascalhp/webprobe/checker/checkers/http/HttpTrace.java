package net.pascalhp.webprobe.checker.checkers.http;

import net.pascalhp.webprobe.ErrorHandler;
import net.pascalhp.webprobe.Report;
import net.pascalhp.webprobe.SSLCertificate;
import net.pascalhp.webprobe.exceptions.ErrorMessage;
import net.pascalhp.webprobe.helpers.StringHelper;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static net.pascalhp.webprobe.Localization.lang;

// TODO: implement basic auth
class HttpTrace {
    public static final int TCP_CONNECTION_TIMEOUT = 5; // 5 sec

    public URL url;
    public boolean ignoreSslErrors;

    public boolean isHttps;
    public HttpCheckResult.TCPLayer tcp;
    public HttpCheckResult.TLSLayer tls;
    public HttpCheckResult.HTTPLayer http;
    public long startTime;
    public boolean success = false;

    private Socket socket;
    private Report.Page reportPage;
    private long timingMark = 0;
    private String stopReason;

    public HttpTrace(URL url, boolean ignoreSslErrors) {
        this.url = url;
        this.ignoreSslErrors = ignoreSslErrors;
        this.initConnectionCredentials();
        this.reportPage = new Report.Page("Check " + url.toString() + " (ignoreSslErrors = " + ignoreSslErrors + ")");
    }

    private void initConnectionCredentials() {
        this.tcp = new HttpCheckResult.TCPLayer();
        this.tcp.host = this.url.getHost();

        int port = this.url.getPort();
        String schema = this.url.getProtocol();

        if (schema == null) {
            schema = "";
        }
        schema = schema.toLowerCase();

        if (schema.isEmpty()) {
            this.tcp.port = (port > 0) ? port : 443;
            this.isHttps = (port == 443);
        } else {
            if (schema.equals("http")) {
                this.isHttps = false;
            } else if (schema.equals("https")) {
                this.isHttps = true;
            } else {
                throw new ErrorMessage("Unsupported url schema: " + schema);
            }
            if (port > 0) {
                this.tcp.port = port;
            } else {
                this.tcp.port = this.isHttps ? 443 : 80;
            }
        }
    }

    public Report.Page trace() {
        this.startTime = System.currentTimeMillis();
        this.logStart();

        if (!this.connectTcpReport()) {
            this.logExit();
            return this.reportPage;
        }

        if (this.stopped()) {
            return this.reportPage;
        }

        if (!this.connectTlsReport()) {
            this.logExit();
            return this.reportPage;
        }

        if (this.stopped()) {
            return this.reportPage;
        }

        this.httpRequestReport();
        this.logExit();

        return this.reportPage;
    }

    private void logStart() {
        Map<String, String> data = new HashMap<>();
        data = Map.of(
                "ignoreSslErrors", this.ignoreSslErrors ? lang("yes") : lang("no"),
                "url", this.url.toString()
        );
        this.reportPage.log(lang("check.http.start", data));
    }

    private void logExit() {
        long totalTime = System.currentTimeMillis() - this.startTime;
        this.reportPage.log(lang("check.http.exit", Map.of("totalTime", String.valueOf(totalTime))));
    }

    private boolean connectTcpReport() {
        Map<String, String> data = Map.of("host", this.tcp.host, "port", String.valueOf(this.tcp.port));
        this.reportPage.log(lang("check.http.tcp_connection_start", data));

        if (this.connectTcp()) {
            data = Map.of(
                    "dnsTime", String.valueOf(this.tcp.dnsTimeMs),
                    "connectTime", String.valueOf(this.tcp.connectTimeMs)
            );
            this.reportPage.log(lang("check.http.tcp_connection_ok", data));
        } else {
            this.reportPage.log(lang("check.http.tcp_connection_fail", Map.of("error", ErrorHandler.formatException(this.tcp.error))));
        }
        return this.tcp.isSuccess;
    }

    private boolean connectTcp() {
        try {
            long start;
            Socket sock = new Socket();

            start = System.currentTimeMillis();
            SocketAddress addr = new InetSocketAddress(this.tcp.host, this.tcp.port);
            this.tcp.dnsTimeMs = System.currentTimeMillis() - start;

            start = System.currentTimeMillis();
            sock.connect(addr, TCP_CONNECTION_TIMEOUT * 1000);
            this.tcp.connectTimeMs = System.currentTimeMillis() - start;

            this.tcp.isSuccess = true;
            this.socket = sock;
        } catch (Throwable e) {
            this.tcp.isSuccess = false;
            this.tcp.error = e;
        }
        return this.tcp.isSuccess;
    }

    private boolean connectTlsReport() {
        if (!this.isHttps) {
            return true;
        }
        this.reportPage.log(lang("check.http.tls_connection_start"));
        if (this.connectTls()) {
            this.reportTlsSuccess();
        } else {
            this.reportPage.log(lang("check.http.tls_connection_fail", Map.of("error", ErrorHandler.formatException(tls.error))));
        }
        return this.tls.isSuccess;
    }

    private void reportTlsSuccess() {
        this.reportPage.log(lang("check.http.tls_connection_ok", Map.of("duration", String.valueOf(this.tls.handshakeTimeMs))));

        LinkedList<String> certFilesList = new LinkedList<>();
        for (SSLCertificate cert : this.tls.serverCertificates) {
            try {
                String certPem = cert.saveToString();
                String fileName = cert.detectName();
                if (fileName == null || fileName.isEmpty()) {
                    fileName = StringHelper.md5(certPem);
                }
                fileName += ".pem";
                certFilesList.add(fileName);
                this.reportPage.addFile(fileName, certPem);
            } catch (Throwable e) {
                reportPage.log("Failed to export SSL certificate: " + ErrorHandler.formatException(e));
            }
        }
        String certFiles = String.join(", ", certFilesList);

        this.reportPage.log(lang("check.http.tls_connection_proto", Map.of("proto", this.tls.sslVersion)));
        this.reportPage.log(lang("check.http.tls_connection_algo", Map.of("algo", this.tls.sslCipher)));
        this.reportPage.log(lang("check.http.tls_connection_cert_files", Map.of("files", certFiles)));
    }

    private boolean connectTls() {
        this.tls = new HttpCheckResult.TLSLayer();

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers;

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore)null);
            trustManagers = tmf.getTrustManagers();

            sslContext.init(null, trustManagers, new SecureRandom());
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket)factory.createSocket(this.socket, this.tcp.host, this.tcp.port, true);
            sslSocket.setEnabledProtocols(new String[] {"TLSv1.2", "TLSv1.3"});

            if (!this.ignoreSslErrors) {
                SSLParameters sslParams = sslSocket.getSSLParameters();
                sslParams.setEndpointIdentificationAlgorithm("HTTPS");
                sslSocket.setSSLParameters(sslParams);
                sslSocket.setEnabledProtocols(new String[] {"SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3"});
            }

            long start = System.currentTimeMillis();
            sslSocket.startHandshake();
            this.tls.handshakeTimeMs = System.currentTimeMillis() - start;

            SSLSession session = sslSocket.getSession();
            this.tls.serverCertificates = new LinkedList<>();
            this.tls.sslVersion = session.getProtocol();
            this.tls.sslCipher = session.getCipherSuite();

            for (Certificate cert : session.getPeerCertificates()) {
                if (cert instanceof X509Certificate) {
                    this.tls.serverCertificates.add(new SSLCertificate((X509Certificate)cert));
                }
            }

            this.socket = sslSocket;
            this.tls.isSuccess = true;
        } catch (Throwable e) {
            this.tls.isSuccess = false;
            this.tls.error = e;
        }

        return this.tls.isSuccess;
    }

    private boolean httpRequestReport() {
        this.reportPage.log(lang("check.http.request_start"));
        if (this.httpRequest()) {
            this.reportHttpRequestSuccess();
            this.success = true;
        } else {
            this.reportPage.log(lang("check.http.request_fail", Map.of("error", ErrorHandler.formatException(this.http.error))));
        }
        return this.http.isSuccess;
    }

    private void reportHttpRequestSuccess() {
        this.reportPage.log(lang("check.http.request_ok"));

        Map<String, String> data = Map.of(
                "responseCode", String.valueOf(this.http.response.code),
                "contentType", (this.http.response.contentType == null) ? "" : this.http.response.contentType
        );
        this.reportPage.log(lang("check.http.request_info", data));
        this.reportPage.log(lang("check.http.request_headers_time", Map.of("requestHeadersTime", String.valueOf(this.http.request.durationMs))));
        this.reportPage.log(lang("check.http.response_headers_time", Map.of("responseHeadersTime", String.valueOf(this.http.response.headersTimeMs))));
        this.reportPage.log(lang("check.http.response_body_time", Map.of("responseBodyTime", String.valueOf(this.http.response.bodyTimeMs))));

        this.reportPage.addFile("request.txt", this.http.request.full);
        this.reportPage.addFile("response_head.txt", this.http.response.welcomeString + "\n" + this.http.response.headers.toString());

        String ext = this.detectExtensionByContent(this.http.response);
        this.reportPage.addFile("response_body." + ext, this.http.response.body);
    }

    // TODO: use mime type db
    private String detectExtensionByContent(Response resp) {
        if (resp.body.length < 1) {
            return "bin";
        }
        if (resp.contentType == null) {
            return "bin";
        }
        if (resp.contentType.startsWith("text/html")) {
            return "html";
        }
        if (resp.contentType.startsWith("text/plain")) {
            return "txt";
        }
        if (resp.contentType.startsWith("text/xml")) {
            return "xml";
        }
        if (resp.contentType.startsWith("image/jpeg")) {
            return "jpg";
        }
        if (resp.contentType.startsWith("image/png")) {
            return "png";
        }
        if (resp.contentType.startsWith("image/gif")) {
            return "gif";
        }
        if (resp.contentType.startsWith("image/webp")) {
            return "webp";
        }
        if (resp.contentType.startsWith("application/json")) {
            return "json";
        }
        return "bin";
    }

    private void timing(String label) {
        if (this.timingMark != 0) {
            long delta = System.currentTimeMillis() - this.timingMark;
            this.reportPage.log(label + ": " + delta);
        }
        this.timingMark = System.currentTimeMillis();
    }

    private boolean httpRequest() {
        this.http = new HttpCheckResult.HTTPLayer();
        try {
            this.http.request = new Request(this.socket, this.url);
            this.http.response = this.http.request.execute();
            this.http.isSuccess = true;
        } catch (Throwable e) {
            this.http.isSuccess = false;
            this.http.error = e;
        }
        return this.http.isSuccess;
    }

    public void stop(String reason) throws IOException {
        if (this.socket != null) {
            this.socket.close();
        }
        this.stopReason = reason;
    }

    private boolean stopped() {
        if (this.stopReason == null) {
            return false;
        } else {
            this.reportPage.log(lang("check.stop_reason", Map.of("reason", this.stopReason)));
            return true;
        }
    }

    /*
    private static class IgnoringErrorTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
    */
}
