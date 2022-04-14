
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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.LineWrapOutputStream;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.filter.SmtpDataStuffing;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.XOAuth2ChallengeParser;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
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
    private SmtpResponseParser responseParser;

    private boolean is8bitEncodingAllowed;
    private boolean isEnhancedStatusCodesProvided;
    private int largestAcceptableMessage;
    private boolean retryXoauthWithNewToken;
    private boolean isPipeliningSupported;

    private final SmtpLogger logger = new SmtpLogger() {
        @Override
        public void log(@NonNull String message, @Nullable Object... args) {
            log(null, message, args);
        }

        @Override
        public boolean isRawProtocolLoggingEnabled() {
            return K9MailLib.isDebug();
        }

        @Override
        public void log(@Nullable Throwable throwable, @NonNull String message, @Nullable Object... args) {
            Timber.v(throwable, message, args);
        }
    };

    public SmtpTransport(ServerSettings serverSettings,
            TrustedSocketFactory trustedSocketFactory, OAuth2TokenProvider oauthTokenProvider) {
        if (!serverSettings.type.equals("smtp")) {
            throw new IllegalArgumentException("Expected SMTP StoreConfig!");
        }

        host = serverSettings.host;
        port = serverSettings.port;

        connectionSecurity = serverSettings.connectionSecurity;

        authType = serverSettings.authenticationType;
        username = serverSettings.username;
        password = serverSettings.password;
        clientCertificateAlias = serverSettings.clientCertificateAlias;

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
            responseParser = new SmtpResponseParser(logger, inputStream);
            outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024);

            readGreeting();

            String hostnameToReportInHelo = buildHostnameToReport();

            Map<String, List<String>> extensions = sendHello(hostnameToReportInHelo);

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
                    responseParser = new SmtpResponseParser(logger, inputStream);
                    outputStream = new BufferedOutputStream(socket.getOutputStream(), 1024);
                    /*
                     * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically,
                     * Exim.
                     */
                    extensions = sendHello(hostnameToReportInHelo);
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
            List<String> saslMech = extensions.get("AUTH");
            if (saslMech != null) {
                authLoginSupported = saslMech.contains("LOGIN");
                authPlainSupported = saslMech.contains("PLAIN");
                authCramMD5Supported = saslMech.contains("CRAM-MD5");
                authExternalSupported = saslMech.contains("EXTERNAL");
                authXoauth2Supported = saslMech.contains("XOAUTH2");
            }
            parseOptionalSizeValue(extensions.get("SIZE"));

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

    private void readGreeting() {
        SmtpResponse smtpResponse = responseParser.readGreeting();
        logResponse(smtpResponse, false);
    }

    private void logResponse(SmtpResponse smtpResponse, boolean omitText) {
        if (K9MailLib.isDebug()) {
            Timber.v("%s", smtpResponse.toLogString(omitText, "SMTP <<< "));
        }
    }

    private String buildHostnameToReport() {
        InetAddress localAddress = socket.getLocalAddress();

        // we use local ip statically for privacy reasons, see https://github.com/k9mail/k-9/pull/3798
        if (localAddress instanceof Inet6Address) {
            return "[IPv6:::1]";
        } else {
            return "[127.0.0.1]";
        }
    }

    private void parseOptionalSizeValue(List<String> sizeParameters) {
        if (sizeParameters != null && sizeParameters.size() >= 1) {
            String sizeParameter = sizeParameters.get(0);
            try {
                largestAcceptableMessage = Integer.parseInt(sizeParameter);
            } catch (NumberFormatException e) {
                if (K9MailLib.isDebug() && DEBUG_PROTOCOL_SMTP) {
                    Timber.d(e, "Tried to parse %s and get an int", sizeParameter);
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
     * @return A (possibly empty) {@code Map<String, List<String>>} of extensions (upper case) and
     * their parameters (possibly empty) as returned by the EHLO command.
     */
    private Map<String, List<String>> sendHello(String host) throws IOException, MessagingException {
        writeLine("EHLO " + host, false);

        SmtpHelloResponse helloResponse = responseParser.readHelloResponse();
        logResponse(helloResponse.getResponse(), false);

        if (helloResponse instanceof SmtpHelloResponse.Hello) {
            SmtpHelloResponse.Hello hello = (SmtpHelloResponse.Hello) helloResponse;

            return hello.getKeywords();
        } else {
            if (K9MailLib.isDebug()) {
                Timber.v("Server doesn't support the EHLO command. Trying HELO...");
            }

            try {
                executeCommand("HELO %s", host);
            } catch (NegativeSmtpReplyException e2) {
                Timber.w("Server doesn't support the HELO command. Continuing anyway.");
            }

            return new HashMap<>();
        }
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        Set<String> addresses = new LinkedHashSet<>();
        for (Address address : message.getRecipients(RecipientType.TO)) {
            addresses.add(address.getAddress());
        }
        for (Address address : message.getRecipients(RecipientType.CC)) {
            addresses.add(address.getAddress());
        }
        for (Address address : message.getRecipients(RecipientType.BCC)) {
            addresses.add(address.getAddress());
        }
        message.removeHeader("Bcc");

        if (addresses.isEmpty()) {
            return;
        }

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

                executePipelinedCommands(pipelinedCommands);
                readPipelinedResponse(pipelinedCommands);
            } else {
                executeCommand(mailFrom);

                for (String address : addresses) {
                    executeCommand("RCPT TO:<%s>", address);
                }
            }

            executeCommand("DATA");

            EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(
                    new LineWrapOutputStream(new SmtpDataStuffing(outputStream), 1000));

            message.writeTo(msgOut);
            msgOut.endWithCrLfAndFlush();

            entireMessageSent = true; // After the "\r\n." is attempted, we may have sent the message
            executeCommand(".");
        } catch (NegativeSmtpReplyException e) {
            throw e;
        } catch (Exception e) {
            throw new MessagingException("Unable to send message", entireMessageSent, e);
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
        responseParser = null;
        outputStream = null;
        socket = null;
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

    private SmtpResponse executeSensitiveCommand(String format, Object... args)
            throws IOException, MessagingException {
        return executeCommand(true, format, args);
    }

    private SmtpResponse executeCommand(String format, Object... args) throws IOException, MessagingException {
        return executeCommand(false, format, args);
    }

    private SmtpResponse executeCommand(boolean sensitive, String format, Object... args)
            throws IOException, MessagingException {
        String command = String.format(Locale.ROOT, format, args);
        writeLine(command, sensitive);

        SmtpResponse response = responseParser.readResponse(isEnhancedStatusCodesProvided);
        logResponse(response, sensitive);

        if (response.isNegativeResponse()) {
            throw buildNegativeSmtpReplyException(response);
        }

        return response;
    }

    private NegativeSmtpReplyException buildNegativeSmtpReplyException(SmtpResponse response) {
        int replyCode = response.getReplyCode();
        StatusCode statusCode = response.getStatusCode();
        String replyText = response.getJoinedText();

        if (statusCode != null) {
            return new EnhancedNegativeSmtpReplyException(replyCode, replyText, statusCode);
        } else {
            return new NegativeSmtpReplyException(replyCode, replyText);
        }
    }

    private void executePipelinedCommands(Queue<String> pipelinedCommands) throws IOException {
        for (String command : pipelinedCommands) {
            writeLine(command, false);
        }
    }

    private void readPipelinedResponse(Queue<String> pipelinedCommands) throws IOException, MessagingException {
        boolean omitText = false;
        MessagingException firstException = null;

        for (int i = 0, size = pipelinedCommands.size(); i < size; i++) {
            SmtpResponse response = responseParser.readResponse(isEnhancedStatusCodesProvided);
            logResponse(response, omitText);

            if (response.isNegativeResponse() && firstException == null) {
                firstException = buildNegativeSmtpReplyException(response);
            }
        }

        if (firstException != null) {
            throw firstException;
        }
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

        List<String> respList = executeCommand("AUTH CRAM-MD5").getTexts();
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
        SmtpResponse response = executeSensitiveCommand("AUTH XOAUTH2 %s", authString);

        if (response.getReplyCode() == SMTP_CONTINUE_REQUEST) {
            String replyText = response.getJoinedText();
            retryXoauthWithNewToken = XOAuth2ChallengeParser.shouldRetry(replyText, host);

            //Per Google spec, respond to challenge with empty response
            executeCommand("");
        }
    }

    private void saslAuthExternal() throws MessagingException, IOException {
        executeCommand("AUTH EXTERNAL %s", Base64.encode(username));
    }

    public void checkSettings() throws MessagingException {
        close();
        try {
            open();
        } finally {
            close();
        }
    }
}
