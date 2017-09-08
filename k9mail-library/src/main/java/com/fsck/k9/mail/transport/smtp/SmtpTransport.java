
package com.fsck.k9.mail.transport.smtp;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.Authentication;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportUris;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.LineWrapOutputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.filter.SmtpDataStuffing;
import com.fsck.k9.mail.internet.CharsetSupport;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.XOAuth2ChallengeParser;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import javax.net.ssl.SSLException;
import org.apache.commons.io.IOUtils;
import timber.log.Timber;

import static com.fsck.k9.mail.CertificateValidationException.Reason.MissingCapability;
import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_SMTP;

public class SmtpTransport extends Transport {
    private static final int SMTP_CONTINUE_REQUEST = 334;
    private static final int SMTP_AUTHENTICATION_FAILURE_ERROR_CODE = 535;


    private final TrustedSocketFactory trustedSocketFactory;
    private final OAuth2TokenProvider oauthTokenProvider;

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String clientCertificateAlias;
    private final AuthType authType;
    private final ConnectionSecurity connectionSecurity;


    private Socket socket;
    private PeekableInputStream inputStream;
    private OutputStream outputStream;
    private boolean is8bitEncodingAllowed;
    private boolean isEnhancedStatusCodesProvided;
    private int largestAcceptableMessage;
    private boolean retryXoauthWithNewToken;
    private boolean isPipeliningSupported;


    public SmtpTransport(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory,
            OAuth2TokenProvider oauthTokenProvider) throws MessagingException {
        ServerSettings settings;
        try {
            settings = TransportUris.decodeTransportUri(storeConfig.getTransportUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding transport URI", e);
        }

        if (settings.type != Type.SMTP) {
            throw new IllegalArgumentException("Expected SMTP StoreConfig!");
        }

        host = settings.host;
        port = settings.port;

        connectionSecurity = settings.connectionSecurity;

        authType = settings.authenticationType;
        username = settings.username;
        password = settings.password;
        clientCertificateAlias = settings.clientCertificateAlias;

        this.trustedSocketFactory = trustedSocketFactory;
        this.oauthTokenProvider = oauthTokenProvider;
    }

    @Override
    public void open() throws MessagingException {
        try {
            boolean secureConnection = false;
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (int i = 0; i < addresses.length; i++) {
                try {
                    SocketAddress socketAddress = new InetSocketAddress(addresses[i], port);
                    if (connectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
                        socket = trustedSocketFactory.createSocket(null, host, port, clientCertificateAlias);
                        socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                        secureConnection = true;
                    } else {
                        socket = new Socket();
                        socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
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
            socket.setSoTimeout(SOCKET_READ_TIMEOUT);

            inputStream = new PeekableInputStream(new BufferedInputStream(socket.getInputStream(), 1024));
            outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024);

            // Eat the banner
            executeCommand(null);

            InetAddress localAddress = socket.getLocalAddress();
            String localHost = getCanonicalHostName(localAddress);
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

            Map<String, String> extensions = sendHello(localHost);

            is8bitEncodingAllowed = extensions.containsKey("8BITMIME");
            isEnhancedStatusCodesProvided = extensions.containsKey("ENHANCEDSTATUSCODES");
            isPipeliningSupported = extensions.containsKey("PIPELINING");

            if (connectionSecurity == ConnectionSecurity.STARTTLS_REQUIRED) {
                if (extensions.containsKey("STARTTLS")) {
                    executeCommand("STARTTLS");

                    socket = trustedSocketFactory.createSocket(
                            socket,
                            host,
                            port,
                            clientCertificateAlias);

                    inputStream = new PeekableInputStream(new BufferedInputStream(socket.getInputStream(),
                            1024));
                    outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024);
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
            boolean authXoauth2Supported = false;
            if (extensions.containsKey("AUTH")) {
                List<String> saslMech = Arrays.asList(extensions.get("AUTH").split(" "));
                authLoginSupported = saslMech.contains("LOGIN");
                authPlainSupported = saslMech.contains("PLAIN");
                authCramMD5Supported = saslMech.contains("CRAM-MD5");
                authExternalSupported = saslMech.contains("EXTERNAL");
                authXoauth2Supported = saslMech.contains("XOAUTH2");
            }
            parseOptionalSizeValue(extensions);

            if (!TextUtils.isEmpty(username)
                    && (!TextUtils.isEmpty(password) ||
                    AuthType.EXTERNAL == authType ||
                    AuthType.XOAUTH2 == authType)) {

                switch (authType) {

                /*
                 * LOGIN is an obsolete option which is unavailable to users,
                 * but it still may exist in a user's settings from a previous
                 * version, or it may have been imported.
                 */
                    case LOGIN:
                    case PLAIN:
                        // try saslAuthPlain first, because it supports UTF-8 explicitly
                        if (authPlainSupported) {
                            saslAuthPlain();
                        } else if (authLoginSupported) {
                            saslAuthLogin();
                        } else {
                            throw new MessagingException(
                                    "Authentication methods SASL PLAIN and LOGIN are unavailable.");
                        }
                        break;

                    case CRAM_MD5:
                        if (authCramMD5Supported) {
                            saslAuthCramMD5();
                        } else {
                            throw new MessagingException("Authentication method CRAM-MD5 is unavailable.");
                        }
                        break;
                    case XOAUTH2:
                        if (authXoauth2Supported && oauthTokenProvider != null) {
                            saslXoauth2();
                        } else {
                            throw new MessagingException("Authentication method XOAUTH2 is unavailable.");
                        }
                        break;
                    case EXTERNAL:
                        if (authExternalSupported) {
                            saslAuthExternal();
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
                                saslAuthPlain();
                            } else if (authLoginSupported) {
                                saslAuthLogin();
                            } else if (authCramMD5Supported) {
                                saslAuthCramMD5();
                            } else {
                                throw new MessagingException("No supported authentication methods available.");
                            }
                        } else {
                            if (authCramMD5Supported) {
                                saslAuthCramMD5();
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
                        throw new MessagingException(
                                "Unhandled authentication method found in the server settings (bug).");
                }
            }
        } catch (MessagingException e) {
            close();
            throw e;
        } catch (SSLException e) {
            close();
            throw new CertificateValidationException(e.getMessage(), e);
        } catch (GeneralSecurityException gse) {
            close();
            throw new MessagingException(
                "Unable to open connection to SMTP server due to security error.", gse);
        } catch (IOException ioe) {
            close();
            throw new MessagingException("Unable to open connection to SMTP server.", ioe);
        }
    }

    private void parseOptionalSizeValue(Map<String, String> extensions) {
        if (extensions.containsKey("SIZE")) {
            String optionalsizeValue = extensions.get("SIZE");
            if (optionalsizeValue != null && !"".equals(optionalsizeValue)) {
                try {
                    largestAcceptableMessage = Integer.parseInt(optionalsizeValue);
                } catch (NumberFormatException e) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
                        Timber.d(e, "Tried to parse %s and get an int", optionalsizeValue);
                    }
                }
            }
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
    private Map<String, String> sendHello(String host) throws IOException, MessagingException {
        Map<String, String> extensions = new HashMap<>();
        try {
            List<String> results = executeCommand("EHLO %s", host).results;
            // Remove the EHLO greeting response
            results.remove(0);
            for (String result : results) {
                String[] pair = result.split(" ", 2);
                extensions.put(pair[0].toUpperCase(Locale.US), pair.length == 1 ? "" : pair[1]);
            }
        } catch (NegativeSmtpReplyException e) {
            if (K9MailLib.isDebug()) {
                Timber.v("Server doesn't support the EHLO command. Trying HELO...");
            }

            try {
                executeCommand("HELO %s", host);
            } catch (NegativeSmtpReplyException e2) {
                Timber.w("Server doesn't support the HELO command. Continuing anyway.");
            }
        }
        return extensions;
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        List<Address> addresses = new ArrayList<>();
        {
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.TO)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.CC)));
            addresses.addAll(Arrays.asList(message.getRecipients(RecipientType.BCC)));
        }
        message.setRecipients(RecipientType.BCC, null);

        Map<String, List<String>> charsetAddressesMap = new HashMap<>();
        for (Address address : addresses) {
            String addressString = address.getAddress();
            String charset = CharsetSupport.getCharsetFromAddress(addressString);
            List<String> addressesOfCharset = charsetAddressesMap.get(charset);
            if (addressesOfCharset == null) {
                addressesOfCharset = new ArrayList<>();
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

        // If the message has attachments and our server has told us about a limit on
        // the size of messages, count the message's size before sending it
        if (largestAcceptableMessage > 0 && message.hasAttachments()) {
            if (message.calculateSize() > largestAcceptableMessage) {
                throw new MessagingException("Message too large for server", true);
            }
        }

        boolean entireMessageSent = false;

        try {
            String mailFrom = constructSmtpMailFromCommand(message.getFrom(), is8bitEncodingAllowed);

            if (isPipeliningSupported) {
                Queue<String> pipelinedCommands = new LinkedList<>();
                pipelinedCommands.add(mailFrom);

                for (String address : addresses) {
                    pipelinedCommands.add(String.format("RCPT TO:<%s>", address));
                }

                pipelinedCommands.add("DATA");
                executePipelinedCommands(pipelinedCommands);
                readPipelinedResponse(pipelinedCommands);
            } else {
                executeCommand(mailFrom);

                for (String address : addresses) {
                    executeCommand("RCPT TO:<%s>", address);
                }

                executeCommand("DATA");
            }

            EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                    new LineWrapOutputStream(new SmtpDataStuffing(outputStream), 1000));

            message.writeTo(msgOut);
            msgOut.endWithCrLfAndFlush();

            entireMessageSent = true; // After the "\r\n." is attempted, we may have sent the message
            executeCommand(".");
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

    private static String constructSmtpMailFromCommand(Address[] from, boolean is8bitEncodingAllowed) {
        String fromAddress = from[0].getAddress();
        if (is8bitEncodingAllowed) {
            return String.format("MAIL FROM:<%s> BODY=8BITMIME", fromAddress);
        } else {
            Timber.d("Server does not support 8bit transfer encoding");
            return String.format("MAIL FROM:<%s>", fromAddress);
        }
    }

    @Override
    public void close() {
        try {
            executeCommand("QUIT");
        } catch (Exception e) {
            // don't care
        }
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
        IOUtils.closeQuietly(socket);
        inputStream = null;
        outputStream = null;
        socket = null;
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int d;
        while ((d = inputStream.read()) != -1) {
            char c = (char) d;
            if (c == '\n') {
                break;
            } else if (c != '\r') {
                sb.append(c);
            }
        }
        String ret = sb.toString();
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP)
            Timber.d("SMTP <<< %s", ret);

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
            Timber.d(commandToLog);
        }

        byte[] data = s.concat("\r\n").getBytes();

        /*
         * Important: Send command + CRLF using just one write() call. Using
         * multiple calls will likely result in multiple TCP packets and some
         * SMTP servers misbehave if CR and LF arrive in separate pakets.
         * See issue 799.
         */
        outputStream.write(data);
        outputStream.flush();
    }

    private static class CommandResponse {

        private final int replyCode;
        private final List<String> results;

        CommandResponse(int replyCode, List<String> results) {
            this.replyCode = replyCode;
            this.results = results;
        }
    }

    private CommandResponse executeSensitiveCommand(String format, Object... args)
            throws IOException, MessagingException {
        return executeCommand(true, format, args);
    }

    private CommandResponse executeCommand(String format, Object... args) throws IOException, MessagingException {
        return executeCommand(false, format, args);
    }

    private CommandResponse executeCommand(boolean sensitive, String format, Object... args)
            throws IOException, MessagingException {
        List<String> results = new ArrayList<>();
        if (format != null) {
            String command = String.format(Locale.ROOT, format, args);
            writeLine(command, sensitive);
        }

        String line = readCommandResponseLine(results);

        int length = line.length();
        if (length < 1) {
            throw new MessagingException("SMTP response is 0 length");
        }

        int replyCode = -1;
        if (length >= 3) {
            try {
                replyCode = Integer.parseInt(line.substring(0, 3));
            } catch (NumberFormatException e) { /* ignore */ }
        }

        char replyCodeCategory = line.charAt(0);
        boolean isReplyCodeErrorCategory = (replyCodeCategory == '4') || (replyCodeCategory == '5');
        if (isReplyCodeErrorCategory) {
            if (isEnhancedStatusCodesProvided) {
                throw buildEnhancedNegativeSmtpReplyException(replyCode, results);
            } else {
                String replyText = TextUtils.join(" ", results);
                throw new NegativeSmtpReplyException(replyCode, replyText);
            }
        }

        return new CommandResponse(replyCode, results);
    }

    private MessagingException buildEnhancedNegativeSmtpReplyException(int replyCode, List<String> results) {
        StatusCodeClass statusCodeClass = null;
        StatusCodeSubject statusCodeSubject = null;
        StatusCodeDetail statusCodeDetail = null;

        String message = "";
        for (String resultLine : results) {
            message += resultLine.split(" ", 2)[1] + " ";
        }
        if (results.size() > 0) {
            String[] statusCodeParts = results.get(0).split(" ", 2)[0].split("\\.");

            statusCodeClass = StatusCodeClass.parse(statusCodeParts[0]);
            statusCodeSubject = StatusCodeSubject.parse(statusCodeParts[1]);
            statusCodeDetail = StatusCodeDetail.parse(statusCodeSubject, statusCodeParts[2]);
        }

        return new EnhancedNegativeSmtpReplyException(replyCode, statusCodeClass, statusCodeSubject, statusCodeDetail,
                message.trim());
    }


    /*
     * Read lines as long as the length is 4 or larger, e.g. "220-banner text here".
     * Shorter lines are either errors of contain only a reply code.
     */
    private String readCommandResponseLine(List<String> results) throws IOException {
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
        return line;
    }

    private void executePipelinedCommands(Queue<String> pipelinedCommands) throws IOException {
        for (String command : pipelinedCommands) {
            writeLine(command, false);
        }
    }

    private void readPipelinedResponse(Queue<String> pipelinedCommands) throws IOException, MessagingException {
        String responseLine;
        List<String> results = new ArrayList<>();
        NegativeSmtpReplyException negativeRecipient = null;
        for (String command : pipelinedCommands) {
            results.clear();
            responseLine = readCommandResponseLine(results);
            try {
                responseLineToCommandResponse(responseLine, results);

            } catch (MessagingException exception) {
                if (command.equals("DATA")) {
                    throw exception;
                }
                if (command.startsWith("RCPT")) {
                    negativeRecipient = (NegativeSmtpReplyException) exception;
                }
            }
        }

        if (negativeRecipient != null) {
            try {
                executeCommand(".");
                throw negativeRecipient;
            } catch (NegativeSmtpReplyException e) {
                throw negativeRecipient;
            }
        }

    }

    private CommandResponse responseLineToCommandResponse(String line, List<String> results) throws MessagingException {
        int length = line.length();
        if (length < 1) {
            throw new MessagingException("SMTP response to line is 0 length");
        }

        int replyCode = -1;
        if (length >= 3) {
            try {
                replyCode = Integer.parseInt(line.substring(0, 3));
            } catch (NumberFormatException e) { /* ignore */ }
        }

        char replyCodeCategory = line.charAt(0);
        boolean isReplyCodeErrorCategory = (replyCodeCategory == '4') || (replyCodeCategory == '5');
        if (isReplyCodeErrorCategory) {
            if (isEnhancedStatusCodesProvided) {
                throw buildEnhancedNegativeSmtpReplyException(replyCode, results);
            } else {
                String replyText = TextUtils.join(" ", results);
                throw new NegativeSmtpReplyException(replyCode, replyText);
            }
        }

        return new CommandResponse(replyCode, results);
    }


    private void saslAuthLogin() throws MessagingException, IOException {
        try {
            executeCommand("AUTH LOGIN");
            executeSensitiveCommand(Base64.encode(username));
            executeSensitiveCommand(Base64.encode(password));
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw new AuthenticationFailedException("AUTH LOGIN failed (" + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthPlain() throws MessagingException, IOException {
        String data = Base64.encode("\000" + username + "\000" + password);
        try {
            executeSensitiveCommand("AUTH PLAIN %s", data);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw new AuthenticationFailedException("AUTH PLAIN failed ("
                        + exception.getMessage() + ")");
            } else {
                throw exception;
            }
        }
    }

    private void saslAuthCramMD5() throws MessagingException, IOException {

        List<String> respList = executeCommand("AUTH CRAM-MD5").results;
        if (respList.size() != 1) {
            throw new MessagingException("Unable to negotiate CRAM-MD5");
        }

        String b64Nonce = respList.get(0);
        String b64CRAMString = Authentication.computeCramMd5(username, password, b64Nonce);

        try {
            executeSensitiveCommand(b64CRAMString);
        } catch (NegativeSmtpReplyException exception) {
            if (exception.getReplyCode() == SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw new AuthenticationFailedException(exception.getMessage(), exception);
            } else {
                throw exception;
            }
        }
    }

    private void saslXoauth2() throws MessagingException, IOException {
        retryXoauthWithNewToken = true;
        try {
            attemptXoauth2(username);
        } catch (NegativeSmtpReplyException negativeResponse) {
            if (negativeResponse.getReplyCode() != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponse;
            }

            oauthTokenProvider.invalidateToken(username);

            if (!retryXoauthWithNewToken) {
                handlePermanentFailure(negativeResponse);
            } else {
                handleTemporaryFailure(username, negativeResponse);
            }
        }
    }

    private void handlePermanentFailure(NegativeSmtpReplyException negativeResponse) throws AuthenticationFailedException {
        throw new AuthenticationFailedException(negativeResponse.getMessage(), negativeResponse);
    }

    private void handleTemporaryFailure(String username, NegativeSmtpReplyException negativeResponseFromOldToken)
        throws IOException, MessagingException {
        // Token was invalid

        //We could avoid this double check if we had a reasonable chance of knowing
        //if a token was invalid before use (e.g. due to expiry). But we don't
        //This is the intended behaviour per AccountManager

        Timber.v(negativeResponseFromOldToken, "Authentication exception, re-trying with new token");
        try {
            attemptXoauth2(username);
        } catch (NegativeSmtpReplyException negativeResponseFromNewToken) {
            if (negativeResponseFromNewToken.getReplyCode() != SMTP_AUTHENTICATION_FAILURE_ERROR_CODE) {
                throw negativeResponseFromNewToken;
            }

            //Okay, we failed on a new token.
            //Invalidate the token anyway but assume it's permanent.
            Timber.v(negativeResponseFromNewToken, "Authentication exception for new token, permanent error assumed");

            oauthTokenProvider.invalidateToken(username);

            handlePermanentFailure(negativeResponseFromNewToken);
        }
    }

    private void attemptXoauth2(String username) throws MessagingException, IOException {
        String token = oauthTokenProvider.getToken(username, OAuth2TokenProvider.OAUTH2_TIMEOUT);
        String authString = Authentication.computeXoauth(username, token);
        CommandResponse response = executeSensitiveCommand("AUTH XOAUTH2 %s", authString);

        if (response.replyCode == SMTP_CONTINUE_REQUEST) {
            String replyText = TextUtils.join("", response.results);
            retryXoauthWithNewToken = XOAuth2ChallengeParser.shouldRetry(replyText, host);

            //Per Google spec, respond to challenge with empty response
            executeCommand("");
        }
    }

    private void saslAuthExternal() throws MessagingException, IOException {
        executeCommand("AUTH EXTERNAL %s", Base64.encode(username));
    }

    @VisibleForTesting
    protected String getCanonicalHostName(InetAddress localAddress) {
        return localAddress.getCanonicalHostName();
    }
}
