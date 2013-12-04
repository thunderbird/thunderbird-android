package com.fsck.k9.mail.store;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;


/**
 * Filter and reorder list of cipher suites and TLS versions.
 *
 * <p>
 * See: <a href="http://op-co.de/blog/posts/android_ssl_downgrade/">http://op-co.de/blog/posts/android_ssl_downgrade/</a>
 * </p>
 */
public class TrustedSocketFactory {
    protected static final String ENABLED_CIPHERS[];
    protected static final String ENABLED_PROTOCOLS[];

    static {
        String preferredCiphers[] = {
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
        };
        String preferredProtocols[] = {
            "TLSv1.2", "TLSv1.1", "TLSv1"
        };

        String[] supportedCiphers = null;
        String[] supportedProtocols = null;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            SSLSocketFactory sf = sslContext.getSocketFactory();
            supportedCiphers = sf.getSupportedCipherSuites();
            SSLSocket sock = (SSLSocket)sf.createSocket();
            supportedProtocols = sock.getSupportedProtocols();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (KeyManagementException kme) {
            kme.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }

        ENABLED_CIPHERS = supportedCiphers == null ? null :
            filterBySupport(preferredCiphers, supportedCiphers);
        ENABLED_PROTOCOLS = supportedProtocols == null ? null :
            filterBySupport(preferredProtocols, supportedProtocols);
    }

    protected static String[] filterBySupport(String[] preferred, String[] supported) {
        List<String> enabled = new ArrayList<String>();
        Set<String> available = new HashSet<String>();
        Collections.addAll(available, supported);

        for (String item : preferred) {
            if (available.contains(item)) enabled.add(item);
        }
        return enabled.toArray(new String[enabled.size()]);
    }

    public static Socket createSocket(SSLContext sslContext) throws IOException {
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        hardenSocket(socket);

        return socket;
    }

    private static void hardenSocket(SSLSocket sock) {
        if (ENABLED_CIPHERS != null) {
            sock.setEnabledCipherSuites(ENABLED_CIPHERS);
        }
        if (ENABLED_PROTOCOLS != null) {
            sock.setEnabledProtocols(ENABLED_PROTOCOLS);
        }
    }
}
