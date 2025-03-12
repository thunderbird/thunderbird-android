package com.fsck.k9.helper;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import app.k9mail.core.common.net.HostNameUtils;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustManagerFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import timber.log.Timber;


public class DefaultTrustedSocketFactory implements TrustedSocketFactory {
    private static final String[] ENABLED_CIPHERS;
    private static final String[] ENABLED_PROTOCOLS;

    private static final String[] DISALLOWED_CIPHERS = {
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
            "TLS_ECDH_RSA_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA",
            "TLS_ECDH_anon_WITH_NULL_SHA",
            "TLS_ECDH_anon_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_NULL_SHA256"
    };

    private static final String[] DISALLOWED_PROTOCOLS = {
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
            Timber.e(e, "Error getting information about available SSL/TLS ciphers and protocols");
        }

        ENABLED_CIPHERS = (enabledCiphers == null) ? null : remove(enabledCiphers, DISALLOWED_CIPHERS);
        ENABLED_PROTOCOLS = (supportedProtocols == null) ? null : remove(supportedProtocols, DISALLOWED_PROTOCOLS);
    }

    private final Context context;
    private final TrustManagerFactory trustManagerFactory;

    public DefaultTrustedSocketFactory(Context context, TrustManagerFactory trustManagerFactory) {
        this.context = context;
        this.trustManagerFactory = trustManagerFactory;
    }

    protected static String[] remove(String[] enabled, String[] disallowed) {
        List<String> items = new ArrayList<>();
        Collections.addAll(items, enabled);

        if (disallowed != null) {
            for (String item : disallowed) {
                items.remove(item);
            }
        }

        return items.toArray(new String[0]);
    }

    @NonNull
    public Socket createSocket(Socket socket, @NonNull String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {

        TrustManager[] trustManagers = new TrustManager[] { trustManagerFactory.getTrustManagerForDomain(host, port) };
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

        SSLSocket sslSocket = (SSLSocket) trustedSocket;

        hardenSocket(sslSocket);

        // RFC 6066 does not permit the use of literal IPv4 or IPv6 addresses as SNI hostnames.
        if (HostNameUtils.INSTANCE.isLegalIPAddress(host) == null) {
            setSniHost(socketFactory, sslSocket, host);
        }

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

    public static void setSniHost(SSLSocketFactory factory, SSLSocket socket, String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory) {
            SSLCertificateSocketFactory sslCertificateSocketFactory = (SSLCertificateSocketFactory) factory;
            sslCertificateSocketFactory.setHostname(socket, hostname);
        } else if (Build.VERSION.SDK_INT >= 24) {
            SSLParameters sslParameters = socket.getSSLParameters();
            List<SNIServerName> sniServerNames = Collections.singletonList(new SNIHostName(hostname));
            sslParameters.setServerNames(sniServerNames);
            socket.setSSLParameters(sslParameters);
        } else {
            setHostnameViaReflection(socket, hostname);
        }
    }

    private static void setHostnameViaReflection(SSLSocket socket, String hostname) {
        try {
            socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
        } catch (Throwable e) {
            Timber.e(e, "Could not call SSLSocket#setHostname(String) method ");
        }
    }
}
