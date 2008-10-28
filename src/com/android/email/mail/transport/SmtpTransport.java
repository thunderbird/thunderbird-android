
package com.android.email.mail.transport;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLException;

import android.util.Config;
import android.util.Log;

import com.android.email.Email;
import com.android.email.PeekableInputStream;
import com.android.email.codec.binary.Base64;
import com.android.email.mail.Address;
import com.android.email.mail.AuthenticationFailedException;
import com.android.email.mail.Message;
import com.android.email.mail.MessagingException;
import com.android.email.mail.Transport;
import com.android.email.mail.CertificateValidationException;
import com.android.email.mail.Message.RecipientType;
import com.android.email.mail.store.TrustManagerFactory;

public class SmtpTransport extends Transport {
    public static final int CONNECTION_SECURITY_NONE = 0;

    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;

    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;

    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;

    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    String mHost;

    int mPort;

    String mUsername;

    String mPassword;

    int mConnectionSecurity;

    boolean mSecure;

    Socket mSocket;

    PeekableInputStream mIn;

    OutputStream mOut;

    /**
     * smtp://user:password@server:port CONNECTION_SECURITY_NONE
     * smtp+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * smtp+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * smtp+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * smtp+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public SmtpTransport(String _uri) throws MessagingException {
        URI uri;
        try {
            uri = new URI(_uri);
        } catch (URISyntaxException use) {
            throw new MessagingException("Invalid SmtpTransport URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme.equals("smtp")) {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            mPort = 25;
        } else if (scheme.equals("smtp+tls")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            mPort = 25;
        } else if (scheme.equals("smtp+tls+")) {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            mPort = 25;
        } else if (scheme.equals("smtp+ssl+")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            mPort = 465;
        } else if (scheme.equals("smtp+ssl")) {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            mPort = 465;
        } else {
            throw new MessagingException("Unsupported protocol");
        }

        mHost = uri.getHost();

        if (uri.getPort() != -1) {
            mPort = uri.getPort();
        }

        if (uri.getUserInfo() != null) {
            String[] userInfoParts = uri.getUserInfo().split(":", 2);
            mUsername = userInfoParts[0];
            if (userInfoParts.length > 1) {
                mPassword = userInfoParts[1];
            }
        }
    }

    public void open() throws MessagingException {
        try {
            SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
            if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                    mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
                sslContext.init(null, new TrustManager[] {
                        TrustManagerFactory.get(mHost, secure)
                }, new SecureRandom());
                mSocket = sslContext.getSocketFactory().createSocket();
                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                mSecure = true;
            } else {
                mSocket = new Socket();
                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
            }

            mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
            mOut = mSocket.getOutputStream();

            // Eat the banner
            executeSimpleCommand(null);

            String localHost = "localhost";
            try {
                InetAddress localAddress = InetAddress.getLocalHost();
                localHost = localAddress.getHostName();
            } catch (Exception e) {
                if (Config.LOGD) {
                    if (Email.DEBUG) {
                        Log.d(Email.LOG_TAG, "Unable to look up localhost");
                    }
                }
            }

            String result = executeSimpleCommand("EHLO " + localHost);

            /*
             * TODO may need to add code to fall back to HELO I switched it from
             * using HELO on non STARTTLS connections because of AOL's mail
             * server. It won't let you use AUTH without EHLO.
             * We should really be paying more attention to the capabilities
             * and only attempting auth if it's available, and warning the user
             * if not.
             */
            if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                    || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                if (result.contains("-STARTTLS")) {
                    executeSimpleCommand("STARTTLS");

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                    sslContext.init(null, new TrustManager[] {
                            TrustManagerFactory.get(mHost, secure)
                    }, new SecureRandom());
                    mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort,
                            true);
                    mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(),
                            1024));
                    mOut = mSocket.getOutputStream();
                    mSecure = true;
                    /*
                     * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically,
                     * Exim.
                     */
                    result = executeSimpleCommand("EHLO " + localHost);
                } else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                    throw new MessagingException("TLS not supported but required");
                }
            }

            /*
             * result contains the results of the EHLO in concatenated form
             */
            boolean authLoginSupported = result.matches(".*AUTH.*LOGIN.*$");
            boolean authPlainSupported = result.matches(".*AUTH.*PLAIN.*$");

            if (mUsername != null && mUsername.length() > 0 && mPassword != null
                    && mPassword.length() > 0) {
                if (authPlainSupported) {
                    saslAuthPlain(mUsername, mPassword);
                }
                else if (authLoginSupported) {
                    saslAuthLogin(mUsername, mPassword);
                }
                else {
                    throw new MessagingException("No valid authentication mechanism found.");
                }
            }
        } catch (SSLException e) {
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (GeneralSecurityException gse) {
            throw new MessagingException(
                    "Unable to open connection to SMTP server due to security error.", gse);
        } catch (IOException ioe) {
            throw new MessagingException("Unable to open connection to SMTP server.", ioe);
        }
    }

    public void sendMessage(Message message) throws MessagingException {
        close();
        open();
        Address[] from = message.getFrom();

        try {
            executeSimpleCommand("MAIL FROM: " + "<" + from[0].getAddress() + ">");
            for (Address address : message.getRecipients(RecipientType.TO)) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            for (Address address : message.getRecipients(RecipientType.CC)) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            for (Address address : message.getRecipients(RecipientType.BCC)) {
                executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
            }
            message.setRecipients(RecipientType.BCC, null);
            executeSimpleCommand("DATA");
            // TODO byte stuffing
            message.writeTo(
                    new EOLConvertingOutputStream(
                            new BufferedOutputStream(mOut, 1024)));
            executeSimpleCommand("\r\n.");
        } catch (IOException ioe) {
            throw new MessagingException("Unable to send message", ioe);
        }
    }

    public void close() {
        try {
            mIn.close();
        } catch (Exception e) {

        }
        try {
            mOut.close();
        } catch (Exception e) {

        }
        try {
            mSocket.close();
        } catch (Exception e) {

        }
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    private String readLine() throws IOException {
        StringBuffer sb = new StringBuffer();
        int d;
        while ((d = mIn.read()) != -1) {
            if (((char)d) == '\r') {
                continue;
            } else if (((char)d) == '\n') {
                break;
            } else {
                sb.append((char)d);
            }
        }
        String ret = sb.toString();
        if (Config.LOGD) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, "<<< " + ret);
            }
        }
        return ret;
    }

    private void writeLine(String s) throws IOException {
        if (Config.LOGD) {
            if (Email.DEBUG) {
                Log.d(Email.LOG_TAG, ">>> " + s);
            }
        }
        mOut.write(s.getBytes());
        mOut.write('\r');
        mOut.write('\n');
        mOut.flush();
    }

    private String executeSimpleCommand(String command) throws IOException, MessagingException {
        if (command != null) {
            writeLine(command);
        }

        String line = readLine();

        String result = line;

        while (line.length() >= 4 && line.charAt(3) == '-') {
            line = readLine();
            result += line.substring(3);
        }

        char c = result.charAt(0);
        if ((c == '4') || (c == '5')) {
            throw new MessagingException(result);
        }

        return result;
    }


//    C: AUTH LOGIN
//    S: 334 VXNlcm5hbWU6
//    C: d2VsZG9u
//    S: 334 UGFzc3dvcmQ6
//    C: dzNsZDBu
//    S: 235 2.0.0 OK Authenticated
//
//    Lines 2-5 of the conversation contain base64-encoded information. The same conversation, with base64 strings decoded, reads:
//
//
//    C: AUTH LOGIN
//    S: 334 Username:
//    C: weldon
//    S: 334 Password:
//    C: w3ld0n
//    S: 235 2.0.0 OK Authenticated

    private void saslAuthLogin(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException {
        try {
            executeSimpleCommand("AUTH LOGIN");
            executeSimpleCommand(new String(Base64.encodeBase64(username.getBytes())));
            executeSimpleCommand(new String(Base64.encodeBase64(password.getBytes())));
        }
        catch (MessagingException me) {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
                throw new AuthenticationFailedException("AUTH LOGIN failed (" + me.getMessage()
                        + ")");
            }
            throw me;
        }
    }

    private void saslAuthPlain(String username, String password) throws MessagingException,
            AuthenticationFailedException, IOException {
        byte[] data = ("\000" + username + "\000" + password).getBytes();
        data = new Base64().encode(data);
        try {
            executeSimpleCommand("AUTH PLAIN " + new String(data));
        }
        catch (MessagingException me) {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
                throw new AuthenticationFailedException("AUTH PLAIN failed (" + me.getMessage()
                        + ")");
            }
            throw me;
        }
    }
}
