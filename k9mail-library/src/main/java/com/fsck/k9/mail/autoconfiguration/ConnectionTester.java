package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.apache.commons.io.IOUtils;
import timber.log.Timber;


class ConnectionTester {
    boolean probeIsPortOpen(InetAddress[] inetAddresses, int port) {
        for (InetAddress address : inetAddresses) {
            try {
                Timber.d("Probing %s", port);
                if (connectToAddress(address, port)) {
                    return true;
                }
            } catch (IOException e) {
                Timber.w(e, "Could not connect to %s", address);
            }
        }

        Timber.d("No open port found");
        return false;
    }

    private boolean connectToAddress(InetAddress address, int port) throws IOException {
        SocketAddress socketAddress = new InetSocketAddress(address, port);

        Socket socket = new Socket();
        socket.connect(socketAddress, 1000);

        try {
            return socket.isConnected();
        } finally {
            IOUtils.closeQuietly(socket);
        }
    }

}
