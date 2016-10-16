package com.fsck.k9.mail.helpers;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;


public class TestTrustedSocketFactory implements TrustedSocketFactory {
    @Override
    public Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException {

        TrustManager[] trustManagers = new TrustManager[] { new VeryTrustingTrustManager() };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, null);

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        return sslSocketFactory.createSocket(
                socket,
                socket.getInetAddress().getHostAddress(),
                socket.getPort(),
                true);
    }
}
