package com.fsck.k9.mail.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;

public interface BaseTransport {

    public boolean isSecure();

    public BaseTransport clone();

    public void open();

    public void connect(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException, MessagingException;

    public void connect2(String host, int port, ConnectionSecurity connectionSecurity, int timeoutMilliseconds) throws GeneralSecurityException, IOException;

    public void reopenTls(String host, int port) throws SSLException, GeneralSecurityException, IOException;

    public void setSoTimeout(int timeoutMilliseconds) throws SocketException;

    public boolean isOpen();

    public void close();

    public InputStream getInputStream();

    public OutputStream getOutputStream();

    // TODO: remove it
    public void writeLine(byte[] data) throws IOException;

    public void writeLine(byte[] data, boolean appendCrLf) throws IOException;

    public void write(int oneByte) throws IOException;

    public String readLine() throws IOException;

    public String getLocalHost();

    void useCompression() throws IOException;
}
