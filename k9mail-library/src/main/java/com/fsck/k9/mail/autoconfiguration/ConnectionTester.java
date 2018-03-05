package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.io.IOUtils;
import timber.log.Timber;


class ConnectionTester {
    private static final int PORTSCAN_TIMEOUT = 500;

    boolean isPortOpen(String host, int port) {
        Timber.d("Knocking host %s on port %d", host, port);
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), PORTSCAN_TIMEOUT);
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
