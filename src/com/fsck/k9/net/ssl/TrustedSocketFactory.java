package com.fsck.k9.net.ssl;

import android.util.Log;

import com.fsck.k9.K9;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.*;


/**
 * Filter and reorder list of cipher suites and TLS versions.
 */
public class TrustedSocketFactory {
    protected static final String ENABLED_CIPHERS[];
    protected static final String ENABLED_PROTOCOLS[];

    // Order taken from OpenSSL 1.0.1c
    protected static final String ORDERED_KNOWN_CIPHERS[] = {
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
    };

    protected static final String[] BLACKLISTED_CIPHERS = {
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
    };

    protected static final String ORDERED_KNOWN_PROTOCOLS[] = {
            "TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"
    };

    static {
        String[] enabledCiphers = null;
        String[] enabledProtocols = null;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            SSLSocketFactory sf = sslContext.getSocketFactory();
            SSLSocket sock = (SSLSocket) sf.createSocket();
            enabledCiphers = sock.getEnabledCipherSuites();
            enabledProtocols = sock.getEnabledProtocols();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Error getting information about available SSL/TLS ciphers and " +
                    "protocols", e);
        }

        ENABLED_CIPHERS = (enabledCiphers == null) ? null :
                reorder(enabledCiphers, ORDERED_KNOWN_CIPHERS, BLACKLISTED_CIPHERS);

        ENABLED_PROTOCOLS = (enabledProtocols == null) ? null :
            reorder(enabledProtocols, ORDERED_KNOWN_PROTOCOLS, null);
    }

    protected static String[] reorder(String[] enabled, String[] known, String[] blacklisted) {
        List<String> unknown = new ArrayList<String>();
        Collections.addAll(unknown, enabled);

        // Remove blacklisted items
        if (blacklisted != null) {
            for (String item : blacklisted) {
                unknown.remove(item);
            }
        }

        // Order known items
        List<String> result = new ArrayList<String>();
        for (String item : known) {
            if (unknown.remove(item)) {
                result.add(item);
            }
        }

        // Add unknown items at the end. This way security won't get worse when unknown ciphers
        // start showing up in the future.
        result.addAll(unknown);

        return result.toArray(new String[result.size()]);
    }

    public static Socket createSocket(SSLContext sslContext) throws IOException {
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        hardenSocket(socket);

        return socket;
    }

    public static Socket createSocket(SSLContext sslContext, Socket s, String host, int port,
            boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket(s, host, port, autoClose);
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
