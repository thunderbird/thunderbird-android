package com.fsck.k9.mail.ssl;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.MessagingException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


/**
 * Filter and reorder list of cipher suites and TLS versions.
 */
public class DefaultTrustedSocketFactory implements TrustedSocketFactory {
    protected static final String[] ENABLED_CIPHERS;
    protected static final String[] ENABLED_PROTOCOLS;

    protected static final String[] ORDERED_KNOWN_CIPHERS = {
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
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
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
    };

    protected static final String[] BLACKLISTED_CIPHERS = {
            "SSL_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_RSA_WITH_DES_CBC_SHA",
            "SSL_DHE_DSS_WITH_DES_CBC_SHA",
            "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
            "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_ECDH_RSA_WITH_RC4_128_SHA",
            "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
    };

    protected static final String[] ORDERED_KNOWN_PROTOCOLS = {
            "TLSv1.2", "TLSv1.1", "TLSv1"
    };

    protected static final String[] BLACKLISTED_PROTOCOLS = {
            "SSLv3"
    };

    static {
        String[] enabledCiphers = null;
        String[] supportedProtocols = null;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
            SSLSocketFactory sf = sslContext.getSocketFactory();
            SSLSocket sock = (SSLSocket) sf.createSocket();
            enabledCiphers = sock.getEnabledCipherSuites();

            /*
             * Retrieve all supported protocols, not just the (default) enabled
             * ones. TLSv1.1 & TLSv1.2 are supported on API levels 16+, but are
             * only enabled by default on API levels 20+.
             */
            supportedProtocols = sock.getSupportedProtocols();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error getting information about available SSL/TLS ciphers and " +
                    "protocols", e);
        }

        ENABLED_CIPHERS = (enabledCiphers == null) ? null :
                reorder(enabledCiphers, ORDERED_KNOWN_CIPHERS, BLACKLISTED_CIPHERS);

        ENABLED_PROTOCOLS = (supportedProtocols == null) ? null :
                reorder(supportedProtocols, ORDERED_KNOWN_PROTOCOLS, BLACKLISTED_PROTOCOLS);
    }

    public DefaultTrustedSocketFactory(Context context) {
        this.context = context;
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

    private Context context;

    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {

        TrustManager[] trustManagers = new TrustManager[] { TrustManagerFactory.get(host, port) };
        KeyManager[] keyManagers = null;
        if (!TextUtils.isEmpty(clientCertificateAlias)) {
            keyManagers = new KeyManager[] { new KeyChainKeyManager(context, clientCertificateAlias) };
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        Socket trustedSocket;
        if (socket == null) {
            trustedSocket = socketFactory.createSocket();
        } else {
            trustedSocket = socketFactory.createSocket(socket, host, port, true);
        }
        hardenSocket((SSLSocket) trustedSocket);
        return trustedSocket;
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
