package build

import static Functions.*;
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacertFunctions {
    public static String getFreshCacertStore() {
        File cacertFile = new File(getTargetDir() + "/classes/cacert.jks")
        File cacertTmpFile = new File(getTmpDir() + "/cacert.jks")
        if (isRebuildNeed(cacertTmpFile)) {
            buildCacertStore(cacertTmpFile.toString())
        }
        copyFile(cacertTmpFile, cacertFile)
        return cacertFile.toString()
    }

    private static boolean isRebuildNeed(File cacertTmpFile) {
        if (!cacertTmpFile.isFile()) {
            return true
        }
        long delta = System.currentTimeMillis() - cacertTmpFile.lastModified()
        return delta > 604800000; // 1 week
    }

    public static void buildCacertStore(String storeTo) {
        String url = "https://curl.se/ca/cacert.pem"
        out("Download CA certificates from $url\n")
        String certListPem = httpGet(url).body().string()
        Pattern expr = Pattern.compile("-+BEGIN\\s+CERTIFICATE-+\\s+[\\s\\S]+?\\s+-+END\\s+CERTIFICATE-+\\s+")

        Matcher matcher = expr.matcher(certListPem)
        HashMap<String, X509Certificate> certs = new HashMap<>()
        while (matcher.find()) {
            String certPem = matcher.group(0)
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509")
            ByteArrayInputStream certStream = new ByteArrayInputStream(certPem.getBytes())
            X509Certificate cert = (X509Certificate)certFactory.generateCertificate(certStream)
            String name = getCertName(cert)
            if (name == null) {
                out(certPem)
                throw new RuntimeException("Failed to get certificate name")
            }
            if (certs.containsKey(name)) {
                out(certPem)
                throw new RuntimeException("Non-unique certificate name: $name")
            }
            certs.put(name, cert)
        }

        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null, null)
        certs.forEach { name, cert ->
            store.setCertificateEntry(name, cert)
        }
        OutputStream stream = new FileOutputStream(storeTo)
        store.store(stream, new char[0])
    }

    private static String getCertName(X509Certificate cert) {
        return cert.getSubjectX500Principal().getName()
    }
}
