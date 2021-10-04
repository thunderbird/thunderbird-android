package com.fsck.k9.mail.ssl;


import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import com.fsck.k9.mail.MessagingException;

public interface TrustedSocketFactory {
    Socket createSocket(Socket socket, String host, int port, String clientCertificateAlias)
            throws NoSuchAlgorithmException, KeyManagementException, MessagingException, IOException;
}
