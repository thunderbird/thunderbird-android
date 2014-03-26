package com.fsck.k9.mail.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import org.apache.commons.io.IOUtils;

import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.net.ssl.TrustManagerFactory;
import com.fsck.k9.net.ssl.TrustedSocketFactory;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;

public class MailTransport implements BaseTransport {
    Socket mSocket;
    InputStream mIn;
    OutputStream mOut;

    SocketAddress mSocketAddress;

    int inputBufferSize = 1024;
    int outputBufferSize = 1024;

    public MailTransport setInputBufferSize(int size) {
        inputBufferSize = size;
        return this;
    }

    public MailTransport setOutputBufferSize(int size) {
        outputBufferSize = size;
        return this;
    }

    boolean mSecure = false;

    @Override
    public boolean isSecure() {
        return mSecure;
    }

    public MailTransport() {

    }

    @Override
    public MailTransport clone() {
        return new MailTransport();
    }

    /*
     * Attempts to open a connection using the Uri supplied for connection parameters.  Will attempt
     * an SSL connection if indicated.
     */
    @Override
    public void open() {
    }

    @Override
    public void connect(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException, MessagingException {
        InetAddress[] addresses = getAllByName(host);
        for (int i = 0; i < addresses.length; i++) {
            try {
                SocketAddress socketAddress = new InetSocketAddress(addresses[i], port);
                connectInternal(socketAddress, host, port, connectionSecurity, timeoutMilliseconds);
            }
            catch (SocketException e) {
                if (i < (addresses.length - 1)) {
                    // there are still other addresses for that host to try
                    continue;
                }
                throw new MessagingException("Cannot connect to host", e);
            }
            break; // connection success
        }
    }

    // TODO: is this need?
    @Override
    public void connect2(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException {
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        connectInternal(socketAddress, host, port, connectionSecurity, timeoutMilliseconds);
    }

    private void connectInternal(SocketAddress socketAddress, String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException {
        if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,
                    new TrustManager[] { TrustManagerFactory.get(host, port) },
                    new SecureRandom());
            mSocket = TrustedSocketFactory.createSocket(sslContext);
            mSecure = true;
        }
        else {
            mSocket = new Socket();
            mSecure = false;
        }
        mSocket.connect(socketAddress, timeoutMilliseconds);

        mIn = new BufferedInputStream(mSocket.getInputStream(), inputBufferSize);
        mOut = new BufferedOutputStream(mSocket.getOutputStream(), outputBufferSize);
    }

    @Override
    public void reopenTls(String host, int port) throws SSLException, GeneralSecurityException, IOException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null,
                new TrustManager[] { TrustManagerFactory.get(host, port) },
                new SecureRandom());

        mSocket = TrustedSocketFactory.createSocket(sslContext, mSocket, host, port, true);
        mSecure = true;
        mIn = new BufferedInputStream(mSocket.getInputStream(), inputBufferSize);
        mOut = new BufferedOutputStream(mSocket.getOutputStream(), outputBufferSize);
    }

    /**
     * Set the socket timeout.
     * @param timeoutMilliseconds the read timeout value if greater than {@code 0}, or
     *            {@code 0} for an infinite timeout.
     */
    @Override
    public void setSoTimeout(int timeoutMilliseconds) throws SocketException {
        if (mSocket != null) {
            mSocket.setSoTimeout(timeoutMilliseconds);
        }
    }

    @Override
    public boolean isOpen() {
        return (mIn != null && mOut != null && mSocket != null
                && mSocket.isConnected() && !mSocket.isClosed());
    }

    /**
     * Close the connection.  MUST NOT return any exceptions - must be "best effort" and safe.
     */
    @Override
    public void close() {
        IOUtils.closeQuietly(mIn);
        IOUtils.closeQuietly(mOut);
        IOUtils.closeQuietly(mSocket);
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    @Override
    public InputStream getInputStream() {
        return mIn;
    }

    @Override
    public OutputStream getOutputStream() {
        return mOut;
    }

    // TODO: remove it
    @Override
    public void writeLine(byte[] data) throws IOException {
        writeLine(data, false);
    }

    @Override
    public void writeLine(byte[] data, boolean appendCrLf) throws IOException {
        mOut.write(data);
        if (appendCrLf) {
            mOut.write('\r');
            mOut.write('\n');
        }
        mOut.flush();
    }

    @Override
    public void write(int oneByte) throws IOException {
        mOut.write(oneByte);
    }

    /**
     * Reads a single line from the server, using either \r\n or \n as the delimiter.  The
     * delimiter char(s) are not included in the result.
     */
    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int d = mIn.read();
        if (d == -1) {
            throw new IOException("End of stream reached while trying to read line.");
        }
        do {
            if (((char)d) == '\r') {
                continue;
            }
            else if (((char)d) == '\n') {
                break;
            }
            else {
                sb.append((char)d);
            }
        } while ((d = mIn.read()) != -1);
        return sb.toString();
    }

    private InetAddress[] getAllByName(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    @Override
    public String getLocalHost() {
        InetAddress localAddress = mSocket.getLocalAddress();
        String localHost = localAddress.getCanonicalHostName();
        String ipAddr = localAddress.getHostAddress();

        if (localHost.equals("") || localHost.equals(ipAddr) || localHost.contains("_")) {
            // We don't have a FQDN or the hostname contains invalid
            // characters (see issue 2143), so use IP address.
            if (!ipAddr.equals("")) {
                if (localAddress instanceof Inet6Address) {
                    localHost = "[IPV6:" + ipAddr + "]";
                }
                else {
                    localHost = "[" + ipAddr + "]";
                }
            }
            else {
                // If the IP address is no good, set a sane default (see issue 2750).
                localHost = "android";
            }
        }

        return localHost;
    }

    @Override
    public void useCompression() throws IOException {
        Inflater inf = new Inflater(true);
        InputStream in;
        OutputStream out;

        InflaterInputStream zInputStream = new InflaterInputStream(mSocket.getInputStream(), inf);
        in = new PeekableInputStream(new BufferedInputStream(zInputStream, 1024));

        ZOutputStream zOutputStream = new ZOutputStream(mSocket.getOutputStream(), JZlib.Z_BEST_SPEED, true);
        out = new BufferedOutputStream(zOutputStream, 1024);
        zOutputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);

        mIn = in;
        mOut = out;
    }
}
