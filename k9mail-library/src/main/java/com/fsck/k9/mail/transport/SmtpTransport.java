
package com.fsck.k9.mail.transport;

import android.util.Log;

import com.fsck.k9.mail.*;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.LineWrapOutputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.filter.SmtpDataStuffing;
import com.fsck.k9.mail.internet.CharsetSupport;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;

import javax.net.ssl.SSLException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.security.GeneralSecurityException;
import java.util.*;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_SMTP;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.CertificateValidationException.Reason.MissingCapability;

public class SmtpTransport extends Transport {
    private TrustedSocketFactory mTrustedSocketFactory;

    /**
     * Decodes a SmtpTransport URI.
     *
     * NOTE: In contrast to ImapStore and Pop3Store, the authType is appended at the end!
     *
     * <p>Possible forms:</p>
     * <pre>
     * smtp://user:password:auth@server:port ConnectionSecurity.NONE
     * smtp+tls+://user:password:auth@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * smtp+ssl+://user:password:auth@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static ServerSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authType = null;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;

        URI smtpUri;
        try {
            smtpUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid SmtpTransport URI", use);
        }

        String scheme = smtpUri.getScheme();
        /*
         * Currently available schemes are:
         * smtp
         * smtp+tls+
         * smtp+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * smtp+tls
         * smtp+ssl
         */
        if (scheme.equals("smtp")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultTlsPort;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = smtpUri.getHost();

        if (smtpUri.getPort() != -1) {
            port = smtpUri.getPort();
        }

        if (smtpUri.getUserInfo() != null) {
            String[] userInfoParts = smtpUri.getUserInfo().split(":");
            if (userInfoParts.length == 1) {
                authType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
            } else if (userInfoParts.length == 2) {
                authType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 3) {
                // NOTE: In SmptTransport URIs, the authType comes last!
                authType = AuthType.valueOf(userInfoParts[2]);
                username = decodeUtf8(userInfoParts[0]);
                if (authType == AuthType.EXTERNAL) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[1]);
                } else {
                    password = decodeUtf8(userInfoParts[1]);
                }
            }
        }

        return new ServerSettings(ServerSettings.Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
    }

    /**
     * Creates a SmtpTransport URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A SmtpTransport URI that holds the same information as the {@code server} parameter.
     *
     * @see com.fsck.k9.mail.store.StoreConfig#getTransportUri()
     * @see SmtpTransport#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc = (server.username != null) ?
                encodeUtf8(server.username) : "";
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "smtp+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "smtp+tls+";
                break;
            default:
            case NONE:
                scheme = "smtp";
                break;
        }

        String userInfo;
        AuthType authType = server.authenticationType;
        // NOTE: authType is append at last item, in contrast to ImapStore and Pop3Store!
        if (authType != null) {
            if (AuthType.EXTERNAL == authType) {
                userInfo = userEnc + ":" + clientCertificateAliasEnc + ":" + authType.name();
            } else {
                userInfo = userEnc + ":" + passwordEnc + ":" + authType.name();
            }
        } else {
            userInfo = userEnc + ":" + passwordEnc;
        }
        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create SmtpTransport URI", e);
        }
    }


    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mClientCertificateAlias;
    private AuthType mAuthType;
    private ConnectionSecurity mConnectionSecurity;
    private Socket mSocket;
    private PeekableInputStream mIn;
    private OutputStream mOut;
    private boolean m8bitEncodingAllowed;
    private int mLargestAcceptableMessage;

    public SmtpTransport(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory)
            throws MessagingException {
        ServerSettings settings;
        try {
            settings = decodeUri(storeConfig.getTransportUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding transport URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        mConnectionSecurity = settings.connectionSecurity;

        mAuthType = settings.authenticationType;
        mUsername = settings.username;
        mPassword = settings.password;
        mClientCertificateAlias = settings.clientCertificateAlias;
        mTrustedSocketFactory = trustedSocketFactory;
    }

    @Override
    public void open() throws MessagingException {
        try {
            boolean secureConnection = false;
            InetAddress[] addresses = InetAddress.getAllByName(mHost);
            for (int i = 0; i < addresses.length; i++) {
                try {
                    SocketAddress socketAddress = new InetSocketAddress(addresses[i], mPort);
                    if (mConnectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
                        mSocket = mTrustedSocketFactory.createSocket(null, mHost, mPort, mClientCertificateAlias);
                        mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                        secureConnection = true;
                    } else {
                        mSocket = new Socket();
                        mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                    }
                } catch (SocketException e) {
                    if (i < (addresses.length - 1)) {
                        // there are still other addresses for that host to try
                        continue;
                    }
                    throw new MessagingException("Cannot connect to host", e);
                }
                break; // connection success
            }

            // RFC 1047
            mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);

            mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
            mOut = new BufferedOutputStream(mSocket.getOutputStream(), 1024);

            // Eat the banner
            executeSimpleCommand(null);

            InetAddress localAddress = mSocket.getLocalAddress();
            String localHost = localAddress.getCanonicalHostName();
            String ipAddr = localAddress.getHostAddress();

            if (localHost.equals("") || localHost.equals(ipAddr) || localHost.contains("_")) {
                // We don't have a FQDN or the hostname contains invalid
                // characters (see issue 2143), so use IP address.
                if (!ipAddr.equals("")) {
                    if (localAddress instanceof Inet6Address) {
                        localHost = "[IPv6:" + ipAddr + "]";
                    } else {
                        localHost = "[" + ipAddr + "]";
                    }
                } else {
                    // If the IP address is no good, set a sane default (see issue 2750).
                    localHost = "android";
                }
            }

            Map<String,String> extensions = sendHello(localHost);

            m8bitEncodingAllowed = extensions.containsKey("8BITMIME");


            if (mConnectionSecurity == ConnectionSecurity.STARTTLS_REQUIRED) {
                if (extensions.containsKey("STARTTLS")) {
                    executeSimpleCommand("STARTTLS");

                    mSocket = mTrustedSocketFactory.createSocket(
                            mSocket,
                            mHost,
                            mPort,
                            mClientCertificateAlias);

                    mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(),
                                                  1024));
                    mOut = new BufferedOutputStream(mSocket.getOutputStream(), 1024);
                    /*
                     * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically,
                     * Exim.
                     */
                    extensions = sendHello(localHost);
                    secureConnection = true;
                } else {
                    /*
                     * This exception triggers a "Certificate error"
                     * notification that takes the user to the incoming
                     * server settings for review. This might be needed if
                     * the account was configured with an obsolete
                     * "STARTTLS (if available)" setting.
                     */
                    throw new CertificateValidationException(
                            "STARTTLS connection security not available");
                }
            }

            boolean authLoginSupported = false;
            boolean authPlainSupported = false;
            boolean authCramMD5Supported = false;
            boolean authExternalSupported = false;
            if (extensions.containsKey("AUTH")) {
                List<String> saslMech = Arrays.asList(extensions.get("AUTH").split(" "));
                authLoginSupported = saslMech.contains("LOGIN");
                authPlainSupported = saslMech.contains("PLAIN");
                authCramMD5Supported = saslMech.contains("CRAM-MD5");
                authExternalSupported = saslMech.contains("EXTERNAL");
            }
            if (extensions.containsKey("SIZE")) {
                try {
                    mLargestAcceptableMessage = Integer.parseInt(extensions.get("SIZE"));
                } catch (Exception e) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
                        Log.d(LOG_TAG, "Tried to parse " + extensions.get("SIZE") + " and get an int", e);
                    }
                }
            }

            if (mUsername != null
                    && mUsername.length() > 0
                    && (mPassword != null && mPassword.length() > 0 || AuthType.EXTERNAL == mAuthType)) {

                switch (mAuthType) {

                /*
                 * LOGIN is an obsolete option which is unavailable to users,
                 * but it still may exist in a user's settings from a previous
                 * version, or it may have been imported.
                 */
                case LOGIN:
                case PLAIN:
                    // try saslAuthPlain first, because it supports UTF-8 explicitly
                    if (authPlainSupported) {
                        saslAuthPlain(mUsername, mPassword);
                    } else if (authLoginSupported) {
                        saslAuthLogin(mUsername, mPassword);
                    } else {
                        throw new MessagingException("Authentication methods SASL PLAIN and LOGIN are unavailable.");
                    }
                    break;

                case CRAM_MD5:
                    if (authCramMD5Supported) {
                        saslAuthCramMD5(mUsername, mPassword);
                    } else {
                        throw new MessagingException("Authentication method CRAM-MD5 is unavailable.");
                    }
                    break;

                case EXTERNAL:
                    if (authExternalSupported) {
                        saslAuthExternal(mUsername);
                    } else {
                        /*
                         * Some SMTP servers are known to provide no error
                         * indication when a client certificate fails to
                         * validate, other than to not offer the AUTH EXTERNAL
                         * capability.
                         *
                         * So, we treat it is an error to not offer AUTH
                         * EXTERNAL when using client certificates. That way, the
                         * user can be notified of a problem during account setup.
                         */
                        throw new CertificateValidationException(MissingCapability);
                    }
                    break;

                /*
                 * AUTOMATIC is an obsolete option which is unavailable to users,
                 * but it still may exist in a user's settings from a previous
                 * version, or it may have been imported.
                 */
                case AUTOMATIC:
                    if (secureConnection) {
                        // try saslAuthPlain first, because it supports UTF-8 explicitly
                        if (authPlainSupported) {
                            saslAuthPlain(mUsername, mPassword);
                        } else if (authLoginSupported) {
                            saslAuthLogin(mUsername, mPassword);
                        } else if (authCramMD5Supported) {
                            saslAuthCramMD5(mUsername, mPassword);
                        } else {
                            throw new MessagingException("No supported authentication methods available.");
                        }
                    } else {
                        if (authCramMD5Supported) {
                            saslAuthCramMD5(mUsername, mPassword);
                        } else {
                            /*
                             * We refuse to insecurely transmit the password
                             * using the obsolete AUTOMATIC setting because of
                             * the potential for a MITM attack. Affected users
                             * must choose a different setting.
                             */
                            throw new MessagingException(
                                    "Update your outgoing server authentication setting. AUTOMATIC auth. is unavailable.");
                        }
                    }
                    break;

                default:
                    throw new MessagingException("Unhandled authentication method found in the server settings (bug).");
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

    /**
     * Send the client "identity" using the EHLO or HELO command.
     *
     * <p>
     * We first try the EHLO command. If the server sends a negative response, it probably doesn't
     * support the EHLO command. So we try the older HELO command that all servers need to support.
     * And if that fails, too, we pretend everything is fine and continue unimpressed.
     * </p>
     *
     * @param host
     *         The EHLO/HELO parameter as defined by the RFC.
     *
     * @return A (possibly empty) {@code Map<String,String>} of extensions (upper case) and
     * their parameters (possibly 0 length) as returned by the EHLO command
     *
     * @throws IOException
     *          In case of a network error.
     * @throws MessagingException
     *          In case of a malformed response.
     */
    private Map<String,String> sendHello(String host) throws IOException, MessagingException {
        Map<String, String> extensions = new HashMap<String, String>();
        try {
            List<String> results = executeSimpleCommand("EHLO " + host);
            // Remove the EHLO greeting response
            results.remove(0);
            for (String result : results) {
                String[] pair = result.split(" ", 2);
                extensions.put(pair[0].toUpperCase(Locale.US), pair.length == 1 ? "" : pair[1]);
            }
        } catch (NegativeSmtpReplyException e) {
            if (K9MailLib.isDebug()) {
                Log.v(LOG_TAG, "Server doesn't support the EHLO command. Trying HELO...");
            }

            try {
                executeSimpleCommand("HELO " + host);
            } catch (NegativeSmtpReplyException e2) {
                Log.w(LOG_TAG, "Server doesn't support the HELO command. Continuing anyway.");
            }
        }
        return extensions;
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        List<Address> addresses = new ArrayList<Address>();
        {
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));
        }
        message.setRecipients(RecipientType.BCC, null);

        Map<String, List<String>> charsetAddressesMap =
            new HashMap<String, List<String>>();
        for (Address address : addresses) {
            String addressString = address.getAddress();
            String charset = CharsetSupport.getCharsetFromAddress(addressString);
            List<String> addressesOfCharset = charsetAddressesMap.get(charset);
            if (addressesOfCharset == null) {
                addressesOfCharset = new ArrayList<String>();
                charsetAddressesMap.put(charset, addressesOfCharset);
            }
            addressesOfCharset.add(addressString);
        }

        for (Map.Entry<String, List<String>> charsetAddressesMapEntry :
                charsetAddressesMap.entrySet()) {
            String charset = charsetAddressesMapEntry.getKey();
            List<String> addressesOfCharset = charsetAddressesMapEntry.getValue();
            message.setCharset(charset);
            sendMessageTo(addressesOfCharset, message);
        }
    }

    private void sendMessageTo(List<String> addresses, Message message)
    throws MessagingException {

        close();
        open();

        if (!m8bitEncodingAllowed) {
            message.setUsing7bitTransport();
        }
        // If the message has attachments and our server has told us about a limit on
        // the size of messages, count the message's size before sending it
        if (mLargestAcceptableMessage > 0 && message.hasAttachments()) {
            if (message.calculateSize() > mLargestAcceptableMessage) {
                throw new MessagingException("Message too large for server", true);
            }
        }

        boolean entireMessageSent = false;
        Address[] from = message.getFrom();
        try {
            executeSimpleCommand("MAIL FROM:" + "<" + from[0].getAddress() + ">"
                    + (m8bitEncodingAllowed ? " BODY=8BITMIME" : ""));
            for (String address : addresses) {
                executeSimpleCommand("RCPT TO:" + "<" + address + ">");
            }
            executeSimpleCommand("DATA");

            EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                    new LineWrapOutputStream(new SmtpDataStuffing(mOut), 1000));

            message.writeTo(msgOut);

            // We use BufferedOutputStream. So make sure to call flush() !
            msgOut.flush();

            entireMessageSent = true; // After the "\r\n." is attempted, we may have sent the message
            executeSimpleCommand("\r\n.");
        } catch (NegativeSmtpReplyException e) {
            throw e;
        } catch (Exception e) {
            MessagingException me = new MessagingException("Unable to send message", e);
            me.setPermanentFailure(entireMessageSent);

            throw me;
        } finally {
            close();
        }

    }

    @Override
    public void close() {
        try {
            executeSimpleCommand("QUIT");
        } catch (Exception e) {

        }
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
        StringBuilder sb = new StringBuilder();
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
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP)
            Log.d(LOG_TAG, "SMTP <<< " + ret);

        return ret;
    }

    private void writeLine(String s, boolean sensitive) throws IOException {
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
            final String commandToLog;
            if (sensitive && !K9MailLib.isDebugSensitive()) {
                commandToLog = "SMTP >>> *sensitive*";
            } else {
                commandToLog = "SMTP >>> " + s;
            }
            Log.d(LOG_TAG, commandToLog);
        }

        byte[] data = s.concat("\r\n").getBytes();

        /*
         * Important: Send command + CRLF using just one write() call. Using
         * multiple calls will likely result in multiple TCP packets and some
         * SMTP servers misbehave if CR and LF arrive in separate pakets.
         * See issue 799.
         */
        mOut.write(data);
        mOut.flush();
    }

    private void checkLine(String line) throws MessagingException {
        int length = line.length();
        if (length < 1) {
            throw new MessagingException("SMTP response is 0 length");
        }

        char c = line.charAt(0);
        if ((c == '4') || (c == '5')) {
            int replyCode = -1;
            String message = line;
            if (length >= 3) {
                try {
                    replyCode = Integer.parseInt(line.substring(0, 3));
                } catch (NumberFormatException e) { /* ignore */ }

                if (length > 4) {
                    message = line.substring(4);
                } else {
                    message = "";
                }
            }

            throw new NegativeSmtpReplyException(replyCode, message);
        }
    }

    private List<String> executeSimpleCommand(String command) throws IOException, MessagingException {
        return executeSimpleCommand(command, false);
    }

    private List<String> executeSimpleCommand(String command, boolean sensitive)
    throws IOException, MessagingException {
        List<String> results = new ArrayList<String>();
        if (command != null) {
            writeLine(command, sensitive);
        }

        /*
         * Read lines as long as the length is 4 or larger, e.g. "220-banner text here".
         * Shorter lines are either errors of contain only a reply code. Those cases will
         * be handled by checkLine() below.
         *
         * TODO:  All responses should be checked to confirm that they start with a valid
         * reply code, and that the reply code is appropriate for the command being executed.
         * That means it should either be a 2xx code (generally) or a 3xx code in special cases
         * (e.g., DATA & AUTH LOGIN commands).  Reply codes should be made available as part of
         * the returned object.
         */
        String line = readLine();
        while (line.length() >= 4) {
            if (line.length() > 4) {
                // Everything after the first four characters goes into the results array.
                results.add(line.substring(4));
            }

            if (line.charAt(3) != '-') {
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
        AuthenticationFailedException, IOException {
        try {
            executeSimpleCommand("AUTH LOGIN");
            executeSimpleCommand(Base64.encode(username), true);
            executeSimpleCommand(Base64.encode(password), true);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == 535) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException("AUTH LOGIN failed ("
                        + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthPlain(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException {
        String data = Base64.encode("\000" + username + "\000" + password);
        try {
            executeSimpleCommand("AUTH PLAIN " + data, true);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == 535) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException("AUTH PLAIN failed ("
                        + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthCramMD5(String username, String password) throws MessagingException,
        AuthenticationFailedException, IOException {

        List<String> respList = executeSimpleCommand("AUTH CRAM-MD5");
        if (respList.size() != 1) {
            throw new MessagingException("Unable to negotiate CRAM-MD5");
        }

        String b64Nonce = respList.get(0);
        String b64CRAMString = Authentication.computeCramMd5(mUsername, mPassword, b64Nonce);

        try {
            executeSimpleCommand(b64CRAMString, true);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == 535) {
                // Authentication credentials invalid
                throw new AuthenticationFailedException(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthExternal(String username) throws MessagingException, IOException {
        executeSimpleCommand(
                String.format("AUTH EXTERNAL %s",
                        Base64.encode(username)), false);
    }

    /**
     * Exception that is thrown when the server sends a negative reply (reply codes 4xx or 5xx).
     */
    static class NegativeSmtpReplyException extends MessagingException {
        private static final long serialVersionUID = 8696043577357897135L;

        private final int mReplyCode;
        private final String mReplyText;

        public NegativeSmtpReplyException(int replyCode, String replyText) {
            super("Negative SMTP reply: " + replyCode + " " + replyText, isPermanentSmtpError(replyCode));
            mReplyCode = replyCode;
            mReplyText = replyText;
        }

        private static boolean isPermanentSmtpError(int replyCode) {
            return replyCode >= 500 && replyCode <= 599;
        }

        public int getReplyCode() {
            return mReplyCode;
        }

        public String getReplyText() {
            return mReplyText;
        }
    }
}
