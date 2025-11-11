package net.pascalhp.webprobe;

import net.pascalhp.webprobe.helpers.StringHelper;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SSLCertificate {
    public final X509Certificate cert;

    public SSLCertificate(X509Certificate cert) {
        this.cert = cert;
    }

    public byte[] saveToBytes(boolean raw) throws CertificateException, IOException {
        byte[] certRaw = this.cert.getEncoded();
        if (raw) {
            return certRaw;
        }
        Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        buf.write("-----BEGIN CERTIFICATE-----\n".getBytes());
        buf.write(encoder.encode(certRaw));
        buf.write("\n-----END CERTIFICATE-----\n".getBytes());
        return buf.toByteArray();
    }

    public String saveToString() throws CertificateException, IOException {
        byte[] certBytes = this.saveToBytes(false);
        InputStream stream = new ByteArrayInputStream(certBytes);
        return StringHelper.readStream(stream);
    }

    public String getCommonName() {
        String dn = this.cert.getSubjectX500Principal().getName();

        LdapName ldapDN;
        try {
            ldapDN = new LdapName(dn);
        } catch (InvalidNameException e) {
            return null;
        }

        for (Rdn rdn : ldapDN.getRdns()) {
            if (rdn.getType().equalsIgnoreCase("CN")) {
                return rdn.getValue().toString();
            }
        }
        return null;
    }

    public List<String> getSanDomains() {
        List<String> result = new LinkedList<>();
        try {
            Collection<List<?>> altNames = cert.getSubjectAlternativeNames();
            if (altNames == null) {
                return result;
            }
            for (List<?> item : altNames) {
                Integer type = (Integer)item.get(0);
                Object value = item.get(1);
                if (type == 2) {
                    result.add(value.toString());
                }
            }
        } catch (Throwable e) {
        }
        return result;
    }

    public String detectName() {
        String cn = this.getCommonName();
        if (cn != null && !cn.isEmpty()) {
            return cn;
        }
        List<String> sanDomains = this.getSanDomains();
        if (sanDomains.size() > 0) {
            return sanDomains.get(0);
        }
        return null;
    }
}
