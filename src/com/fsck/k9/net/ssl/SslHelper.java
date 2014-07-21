
package com.fsck.k9.net.ssl;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import android.util.Log;

import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.security.KeyChainKeyManager;

/**
 * Helper class to create SSL sockets with support for client certificate
 * authentication
 */
public class SslHelper {

    private static SSLContext createSslContext(String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException {
        if (K9.DEBUG)
            Log.d(K9.LOG_TAG, "createSslContext: Client certificate alias: "
                    + clientCertificateAlias);

        KeyManager[] keyManagers = new KeyManager[] { new KeyChainKeyManager(
                clientCertificateAlias) };

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
