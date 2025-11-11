package net.pascalhp.webprobe.helpers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertHelper {
    public static X509Certificate load(InputStream pemStream) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate)certFactory.generateCertificate(pemStream);
    }

    public static X509Certificate load(byte[] pemBytes) throws CertificateException {
        InputStream pemStream = new ByteArrayInputStream(pemBytes);
        return load(pemStream);
    }

    public static X509Certificate load(String pem) throws CertificateException {
        InputStream pemStream = new ByteArrayInputStream(pem.getBytes());
        return load(pemStream);
    }
}
