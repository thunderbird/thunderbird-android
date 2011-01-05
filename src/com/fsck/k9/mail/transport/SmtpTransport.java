
package com.fsck.k9.mail.transport;

import android.util.Log;
import com.fsck.k9.K9;
import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.LineWrapOutputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.filter.SmtpDataStuffing;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.store.TrustManagerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SmtpTransport extends Transport
{
    public static final int CONNECTION_SECURITY_NONE = 0;

    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;

    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;

    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;

    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    String mHost;

    int mPort;

    String mUsername;

    String mPassword;

    String mAuthType;

    int mConnectionSecurity;

    boolean mSecure;

    Socket mSocket;

    PeekableInputStream mIn;

    OutputStream mOut;
    private boolean m8bitEncodingAllowed;

    /**
     * smtp://user:password@server:port CONNECTION_SECURITY_NONE
     * smtp+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * smtp+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * smtp+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * smtp+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     *
     * @param _uri
     */
    public SmtpTransport(String _uri) throws MessagingException
    {
        URI uri;
        try
        {
            uri = new URI(_uri);
        }
        catch (URISyntaxException use)
        {
            throw new MessagingException("Invalid SmtpTransport URI", use);
        }

        String scheme = uri.getScheme();
        if (scheme.equals("smtp"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            mPort = 25;
        }
        else if (scheme.equals("smtp+tls"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            mPort = 25;
        }
        else if (scheme.equals("smtp+tls+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            mPort = 25;
        }
        else if (scheme.equals("smtp+ssl+"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            mPort = 465;
        }
        else if (scheme.equals("smtp+ssl"))
        {
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            mPort = 465;
        }
        else
        {
            throw new MessagingException("Unsupported protocol");
        }

        mHost = uri.getHost();

        if (uri.getPort() != -1)
        {
            mPort = uri.getPort();
        }

        if (uri.getUserInfo() != null)
        {
            try
            {
                String[] userInfoParts = uri.getUserInfo().split(":");
                mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
                if (userInfoParts.length > 1)
                {
                    mPassword = URLDecoder.decode(userInfoParts[1], "UTF-8");
                }
                if (userInfoParts.length > 2)
                {
                    mAuthType = userInfoParts[2];
                }
            }
            catch (UnsupportedEncodingException enc)
            {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                Log.e(K9.LOG_TAG, "Couldn't urldecode username or password.", enc);
            }
        }
    }

    @Override
    public void open() throws MessagingException
    {
        try
        {
            SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
            if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                    mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL)
            {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
                sslContext.init(null, new TrustManager[]
                                {
                                    TrustManagerFactory.get(mHost, secure)
                                }, new SecureRandom());
                mSocket = sslContext.getSocketFactory().createSocket();
                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                mSecure = true;
            }
            else
            {
                mSocket = new Socket();
                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
            }

            // RFC 1047
            mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);

            mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
            mOut = mSocket.getOutputStream();

            // Eat the banner
            executeSimpleCommand(null);

            InetAddress localAddress = mSocket.getLocalAddress();
            String localHost = localAddress.getHostName();
            String ipAddr = localAddress.getHostAddress();

            if (localHost.equals("") || localHost.equals(ipAddr) || localHost.contains("_"))
            {
                // We don't have a FQDN or the hostname contains invalid
                // characters (see issue 2143), so use IP address.
                if (!ipAddr.equals(""))
                {
                    if (localAddress instanceof Inet6Address)
                    {
                        localHost = "[IPV6:" + ipAddr + "]";
                    }
                    else
                    {
                        localHost = "[" + ipAddr + "]";
                    }
                }
                else
                {
                    // If the IP address is no good, set a sane default (see issue 2750).
                    localHost = "android";
                }
            }

            List<String> results = executeSimpleCommand("EHLO " + localHost);

            m8bitEncodingAllowed = results.contains("8BITMIME");

            /*
             * TODO may need to add code to fall back to HELO I switched it from
             * using HELO on non STARTTLS connections because of AOL's mail
             * server. It won't let you use AUTH without EHLO.
             * We should really be paying more attention to the capabilities
             * and only attempting auth if it's available, and warning the user
             * if not.
             */
            if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                    || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
            {
                if (results.contains("STARTTLS"))
                {
                    executeSimpleCommand("STARTTLS");

                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                    sslContext.init(null, new TrustManager[]
                                    {
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
                    results = executeSimpleCommand("EHLO " + localHost);
                }
                else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED)
                {
                    throw new MessagingException("TLS not supported but required");
                }
            }

            /*
             * result contains the results of the EHLO in concatenated form
             */
            boolean authLoginSupported = false;
            boolean authPlainSupported = false;
            boolean authCramMD5Supported = false;
            for (String result : results)
            {
                if (result.matches(".*AUTH.*LOGIN.*$"))
                {
                    authLoginSupported = true;
                }
                if (result.matches(".*AUTH.*PLAIN.*$"))
                {
                    authPlainSupported = true;
                }
                if (result.matches(".*AUTH.*CRAM-MD5.*$") && mAuthType != null && mAuthType.equals("CRAM_MD5"))
                {
                    authCramMD5Supported = true;
                }
            }

            if (mUsername != null && mUsername.length() > 0 && mPassword != null
                    && mPassword.length() > 0)
            {
                if (authCramMD5Supported)
                {
                    saslAuthCramMD5(mUsername, mPassword);
                }
                else if (authPlainSupported)
                {
                    saslAuthPlain(mUsername, mPassword);
                }
                else if (authLoginSupported)
                {
                    saslAuthLogin(mUsername, mPassword);
                }
                else
                {
                    throw new MessagingException("No valid authentication mechanism found.");
                }
            }
        }
        catch (SSLException e)
        {
            throw new CertificateValidationException(e.getMessage(), e);
        }
        catch (GeneralSecurityException gse)
        {
            throw new MessagingException(
                "Unable to open connection to SMTP server due to security error.", gse);
        }
        catch (IOException ioe)
        {
            throw new MessagingException("Unable to open connection to SMTP server.", ioe);
        }
    }

    @Override
    public void sendMessage(Message message) throws MessagingException
    {
        ArrayList<Address> addresses = new ArrayList<Address>();
        {
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));
        }
        message.setRecipients(RecipientType.BCC, null);

        HashMap<String, ArrayList<String>> charsetAddressesMap =
                new HashMap<String, ArrayList<String>>();
        for (Address address : addresses)
        {
            String addressString = address.getAddress();
            String charset = MimeUtility.getCharsetFromAddress(addressString);
            ArrayList<String> addressesOfCharset = charsetAddressesMap.get(charset);
            if (addressesOfCharset == null)
            {
                addressesOfCharset = new ArrayList<String>();
                charsetAddressesMap.put(charset, addressesOfCharset);
            }
            addressesOfCharset.add(addressString);
        }

        for (HashMap.Entry<String, ArrayList<String>> charsetAddressesMapEntry :
                     charsetAddressesMap.entrySet())
        {
            String charset = charsetAddressesMapEntry.getKey();
            ArrayList<String> addressesOfCharset = charsetAddressesMapEntry.getValue();
            message.setCharset(charset);
            sendMessageTo(addressesOfCharset, message);
        }
    }

    private void sendMessageTo(ArrayList<String> addresses, Message message)
            throws MessagingException{
        close();
        open();

        message.setEncoding(m8bitEncodingAllowed ? "8bit" : null);

        Address[] from = message.getFrom();
        boolean possibleSend = false;
        try
        {
            //TODO: Add BODY=8BITMIME parameter if appropriate?
            executeSimpleCommand("MAIL FROM: " + "<" + from[0].getAddress() + ">");
            for (String address : addresses)
            {
                executeSimpleCommand("RCPT TO: " + "<" + address + ">");
            }
            executeSimpleCommand("DATA");

            EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                new SmtpDataStuffing(
                    new LineWrapOutputStream(
                        new BufferedOutputStream(mOut, 1024),
                        1000)));

            message.writeTo(msgOut);

            // We use BufferedOutputStream. So make sure to call flush() !
            msgOut.flush();

            possibleSend = true; // After the "\r\n." is attempted, we may have sent the message
            executeSimpleCommand("\r\n.");
        }
        catch (Exception e)
        {
            MessagingException me = new MessagingException("Unable to send message", e);
            me.setPermanentFailure(possibleSend);
            throw me;
        }
        finally
        {
            close();
        }



    }

    @Override
    public void close()
    {
        try
        {
            executeSimpleCommand("QUIT");
        }
        catch (Exception e)
        {

        }
        try
        {
            mIn.close();
        }
        catch (Exception e)
        {

        }
        try
        {
            mOut.close();
        }
        catch (Exception e)
        {

        }
        try
        {
            mSocket.close();
        }
        catch (Exception e)
        {

        }
        mIn = null;
        mOut = null;
        mSocket = null;
    }

    private String readLine() throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int d;
        while ((d = mIn.read()) != -1)
        {
            if (((char)d) == '\r')
            {
                continue;
            }
            else if (((char)d) == '\n')
            {
                break;
            }
            else
            {
                sb.append((char)d);
            }
        }
        String ret = sb.toString();
        if (K9.DEBUG && K9.DEBUG_PROTOCOL_SMTP)
            Log.d(K9.LOG_TAG, "SMTP <<< " + ret);

        return ret;
    }

    private void writeLine(String s, boolean sensitive) throws IOException
    {
        if (K9.DEBUG && K9.DEBUG_PROTOCOL_SMTP)
        {
            final String commandToLog;
            if (sensitive && !K9.DEBUG_SENSITIVE)
            {
                commandToLog = "SMTP >>> *sensitive*";
            }
            else
            {
                commandToLog = "SMTP >>> " + s;
            }
            Log.d(K9.LOG_TAG, commandToLog);
        }

        /*
         * Note: We can use the string length to compute the buffer size since
         * only ASCII characters are allowed in SMTP commands i.e. this string
         * will never contain multi-byte characters.
         */
        int len = s.length();
        byte[] data = new byte[len + 2];
        s.getBytes(0, len, data, 0);
        data[len+0] = '\r';
        data[len+1] = '\n';

        /*
         * Important: Send command + CRLF using just one write() call. Using
         * multiple calls will likely result in multiple TCP packets and some
         * SMTP servers misbehave if CR and LF arrive in separate pakets.
         * See issue 799.
         */
        mOut.write(data);
        mOut.flush();
    }

    private void checkLine(String line) throws MessagingException
    {
        if (line.length() < 1)
        {
            throw new MessagingException("SMTP response is 0 length");
        }
        char c = line.charAt(0);
        if ((c == '4') || (c == '5'))
        {
            throw new MessagingException(line);
        }
    }

    private List<String> executeSimpleCommand(String command) throws IOException, MessagingException
    {
        return executeSimpleCommand(command, false);
    }

    private List<String> executeSimpleCommand(String command, boolean sensitive)
    throws IOException, MessagingException
    {
        List<String> results = new ArrayList<String>();
        if (command != null)
        {
            writeLine(command, sensitive);
        }

        /*
         * Read lines as long as the length is 4 or larger, e.g. "220-banner text here".
         * Shorter lines are either errors of contain only a reply code. Those cases will
         * be handled by checkLine() below.
         */
        String line = readLine();
        while (line.length() >= 4)
        {
            if (line.length() > 4)
            {
                // Everything after the first four characters goes into the results array.
                results.add(line.substring(4));
            }

            if (line.charAt(3) != '-')
            {
                // If the fourth character isn't "-" this is the last line of the response.
                break;
            }
            line = readLine();
        }

        // Check if the reply code indicates an error.
        checkLine(line);

        return results;
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
        AuthenticationFailedException, IOException
    {
        try
        {
            executeSimpleCommand("AUTH LOGIN");
            executeSimpleCommand(new String(Base64.encodeBase64(username.getBytes())), true);
            executeSimpleCommand(new String(Base64.encodeBase64(password.getBytes())), true);
        }
        catch (MessagingException me)
        {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3')
            {
                throw new AuthenticationFailedException("AUTH LOGIN failed (" + me.getMessage()
                                                        + ")");
            }
            throw me;
        }
    }

    private void saslAuthPlain(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException
    {
        byte[] data = ("\000" + username + "\000" + password).getBytes();
        data = new Base64().encode(data);
        try
        {
            executeSimpleCommand("AUTH PLAIN " + new String(data), true);
        }
        catch (MessagingException me)
        {
            if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3')
            {
                throw new AuthenticationFailedException("AUTH PLAIN failed (" + me.getMessage()
                                                        + ")");
            }
            throw me;
        }
    }

    private void saslAuthCramMD5(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException
    {
        List<String> respList = executeSimpleCommand("AUTH CRAM-MD5");
        if (respList.size() != 1) throw new AuthenticationFailedException("Unable to negotiate CRAM-MD5");
        String b64Nonce = respList.get(0);
        byte[] nonce = Base64.decodeBase64(b64Nonce.getBytes("US-ASCII"));
        byte[] ipad = new byte[64];
        byte[] opad = new byte[64];
        byte[] secretBytes = password.getBytes("US-ASCII");
        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException nsae)
        {
            throw new AuthenticationFailedException("MD5 Not Available.");
        }
        if (secretBytes.length > 64)
        {
            secretBytes = md.digest(secretBytes);
        }
        System.arraycopy(secretBytes, 0, ipad, 0, secretBytes.length);
        System.arraycopy(secretBytes, 0, opad, 0, secretBytes.length);
        for (int i = 0; i < ipad.length; i++) ipad[i] ^= 0x36;
        for (int i = 0; i < opad.length; i++) opad[i] ^= 0x5c;
        md.update(ipad);
        byte[] firstPass = md.digest(nonce);
        md.update(opad);
        byte[] result = md.digest(firstPass);
        String plainCRAM = username + " " + new String(Hex.encodeHex(result));
        byte[] b64CRAM = Base64.encodeBase64(plainCRAM.getBytes("US-ASCII"));
        String b64CRAMString = new String(b64CRAM, "US-ASCII");
        try
        {
            executeSimpleCommand(b64CRAMString, true);
        }
        catch (MessagingException me)
        {
            throw new AuthenticationFailedException("Unable to negotiate MD5 CRAM");
        }
    }
}
