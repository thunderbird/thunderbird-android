
package com.fsck.k9.net.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.annotation.SuppressLint;
import android.os.Build;
import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.security.KeyChainKeyManager;

/**
 * Helper class to create SSL sockets with support for client certificate
 * authentication
 */
public class SslHelper {

    /**
     * KeyChain API available on Android >= 4.0
     * 
     * @return true if API is available
     */
    public static boolean isClientCertificateSupportAvailable() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH);
    }

    @SuppressLint("TrulyRandom")
    private static SSLContext createSslContext(String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException {
        if (clientCertificateAlias != null && !isClientCertificateSupportAvailable()) {
            throw new MessagingException(
                    "Client certificate support is only availble on Android >= 4.0", true);
        }

        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "createSslContext: Client certificate alias: "
                    + clientCertificateAlias);

        KeyManager[] keyManagers = null;
        if (clientCertificateAlias != null) {
            keyManagers = new KeyManager[] {
                    new KeyChainKeyManager(clientCertificateAlias)
            };
        } else {
            keyManagers = new KeyManager[] {
                    new KeyChainKeyManager()
            };
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers,
                new TrustManager[] {
                    TrustManagerFactory.get(
                            host, port)
                },
                new SecureRandom());

        return sslContext;
    }

    /**
     * Create SSL socket
     * 
     * @param host
     * @param port
     * @param clientCertificateAlias if not null, uses client certificate
     *            retrieved by this alias for authentication
     */
    public static Socket createSslSocket(String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, IOException,
            MessagingException {
        SSLContext sslContext = createSslContext(host, port, clientCertificateAlias);
        return TrustedSocketFactory.createSocket(sslContext);
    }

    /**
     * Create socket for START_TLS. autoClose = true
     * 
     * @param socket
     * @param host
     * @param port
     * @param secure
     * @param clientCertificateAlias if not null, uses client certificate
     *            retrieved by this alias for authentication
     */
    public static Socket createStartTlsSocket(Socket socket, String host, int port, boolean secure,
            String clientCertificateAlias) throws NoSuchAlgorithmException,
            KeyManagementException, IOException, MessagingException {
        SSLContext sslContext = createSslContext(host, port, clientCertificateAlias);
        boolean autoClose = true;
        return TrustedSocketFactory.createSocket(sslContext, socket, host, port, autoClose);
    }
}
