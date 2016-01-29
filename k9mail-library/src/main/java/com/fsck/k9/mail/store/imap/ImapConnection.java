package com.fsck.k9.mail.store.imap;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.Authentication;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.PeekableInputStream;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZOutputStream;
import javax.net.ssl.SSLException;
import org.apache.commons.io.IOUtils;

import static com.fsck.k9.mail.ConnectionSecurity.STARTTLS_REQUIRED;
import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_IMAP;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.store.RemoteStore.SOCKET_CONNECT_TIMEOUT;
import static com.fsck.k9.mail.store.RemoteStore.SOCKET_READ_TIMEOUT;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_AUTH_CRAM_MD5;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_AUTH_EXTERNAL;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_AUTH_PLAIN;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_COMPRESS_DEFLATE;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_IDLE;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_LOGINDISABLED;
import static com.fsck.k9.mail.store.imap.ImapCommands.CAPABILITY_NAMESPACE;
import static com.fsck.k9.mail.store.imap.ImapCommands.COMMAND_CAPABILITY;
import static com.fsck.k9.mail.store.imap.ImapCommands.COMMAND_COMPRESS_DEFLATE;
import static com.fsck.k9.mail.store.imap.ImapCommands.COMMAND_NAMESPACE;
import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


/**
 * A cacheable class that stores the details for a single IMAP connection.
 */
class ImapConnection {
    private static final int BUFFER_SIZE = 1024;


    private final ConnectivityManager connectivityManager;
    private final TrustedSocketFactory socketFactory;
    private final int socketConnectTimeout;
    private final int socketReadTimeout;

    private Socket socket;
    private PeekableInputStream inputStream;
    private OutputStream outputStream;
    private ImapResponseParser responseParser;
    private int nextCommandTag;
    private Set<String> capabilities = new HashSet<String>();
    private ImapSettings settings;


    public ImapConnection(ImapSettings settings, TrustedSocketFactory socketFactory,
            ConnectivityManager connectivityManager) {
        this.settings = settings;
        this.socketFactory = socketFactory;
        this.connectivityManager = connectivityManager;
        this.socketConnectTimeout = SOCKET_CONNECT_TIMEOUT;
        this.socketReadTimeout = SOCKET_READ_TIMEOUT;
    }

    ImapConnection(ImapSettings settings, TrustedSocketFactory socketFactory, ConnectivityManager connectivityManager,
            int socketConnectTimeout, int socketReadTimeout) {
        this.settings = settings;
        this.socketFactory = socketFactory;
        this.connectivityManager = connectivityManager;
        this.socketConnectTimeout = socketConnectTimeout;
        this.socketReadTimeout = socketReadTimeout;
    }

    public void open() throws IOException, MessagingException {
        if (isOpen()) {
            return;
        }

        boolean authSuccess = false;
        nextCommandTag = 1;
        adjustDNSCacheTTL();

        try {
            socket = connect(settings, socketFactory);
            setReadTimeout(socketReadTimeout);

            inputStream = new PeekableInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
            responseParser = new ImapResponseParser(inputStream);
            outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);

            capabilities.clear();
            ImapResponse nullResponse = responseParser.readResponse();

            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                Log.v(LOG_TAG, getLogId() + "<<<" + nullResponse);
            }

            List<ImapResponse> nullResponses = new LinkedList<ImapResponse>();
            nullResponses.add(nullResponse);
            receiveCapabilities(nullResponses);

            if (capabilities.isEmpty()) {
                if (K9MailLib.isDebug()) {
                    Log.i(LOG_TAG, "Did not get capabilities in banner, requesting CAPABILITY for " + getLogId());
                }

                List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
                if (responses.size() != 2) {
                    throw new MessagingException("Invalid CAPABILITY response received");
                }
            }

            if (settings.getConnectionSecurity() == STARTTLS_REQUIRED) {
                if (hasCapability("STARTTLS")) {
                    startTLS();
                } else {
                    /*
                     * This exception triggers a "Certificate error"
                     * notification that takes the user to the incoming
                     * server settings for review. This might be needed if
                     * the account was configured with an obsolete
                     * "STARTTLS (if available)" setting.
                     */
                    throw new CertificateValidationException("STARTTLS connection security not available");
                }
            }

            authenticate(settings.getAuthType());
            authSuccess = true;

            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, CAPABILITY_COMPRESS_DEFLATE + " = " + hasCapability(CAPABILITY_COMPRESS_DEFLATE));
            }

            if (hasCapability(CAPABILITY_COMPRESS_DEFLATE) && shouldEnableCompression()) {
                enableCompression();
            }

            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, "NAMESPACE = " + hasCapability(CAPABILITY_NAMESPACE) +
                        ", mPathPrefix = " + settings.getPathPrefix());
            }

            if (settings.getPathPrefix() == null) {
                if (hasCapability(CAPABILITY_NAMESPACE)) {
                    if (K9MailLib.isDebug()) {
                        Log.i(LOG_TAG, "pathPrefix is unset and server has NAMESPACE capability");
                    }
                    handleNamespace();
                } else {
                    if (K9MailLib.isDebug()) {
                        Log.i(LOG_TAG, "pathPrefix is unset but server does not have NAMESPACE capability");
                    }
                    settings.setPathPrefix("");
                }
            }

            if (settings.getPathDelimiter() == null) {
                getPathDelimiter();
            }

        } catch (SSLException e) {
            if (e.getCause() instanceof CertificateException) {
                throw new CertificateValidationException(e.getMessage(), e);
            } else {
                throw e;
            }
        } catch (GeneralSecurityException e) {
            throw new MessagingException("Unable to open connection to IMAP server due to security error.", e);
        } catch (ConnectException e) {
            String message = e.getMessage();
            String[] tokens = message.split("-");

            if (tokens.length > 1 && tokens[1] != null) {
                Log.e(LOG_TAG, "Stripping host/port from ConnectionException for " + getLogId(), e);
                throw new ConnectException(tokens[1].trim());
            } else {
                throw e;
            }
        } finally {
            if (!authSuccess) {
                Log.e(LOG_TAG, "Failed to login, closing connection for " + getLogId());
                close();
            }
        }
    }

    public boolean isOpen() {
        return inputStream != null && outputStream != null && socket != null &&
                socket.isConnected() && !socket.isClosed();
    }

    private void adjustDNSCacheTTL() {
        try {
            Security.setProperty("networkaddress.cache.ttl", "0");
        } catch (Exception e) {
            Log.w(LOG_TAG, "Could not set DNS ttl to 0 for " + getLogId(), e);
        }

        try {
            Security.setProperty("networkaddress.cache.negative.ttl", "0");
        } catch (Exception e) {
            Log.w(LOG_TAG, "Could not set DNS negative ttl to 0 for " + getLogId(), e);
        }
    }

    private Socket connect(ImapSettings settings, TrustedSocketFactory socketFactory) throws GeneralSecurityException,
            MessagingException, IOException {
        Exception connectException = null;

        String host = settings.getHost();
        int port = settings.getPort();
        String clientCertificateAlias = settings.getClientCertificateAlias();

        InetAddress[] inetAddresses = InetAddress.getAllByName(host);
        for (InetAddress address : inetAddresses) {
            try {
                if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                    Log.d(LOG_TAG, "Connecting to " + host + " as " + address);
                }

                SocketAddress socketAddress = new InetSocketAddress(address, port);

                Socket socket;
                if (settings.getConnectionSecurity() == ConnectionSecurity.SSL_TLS_REQUIRED) {
                    socket = socketFactory.createSocket(null, host, port, clientCertificateAlias);
                } else {
                    socket = new Socket();
                }

                socket.connect(socketAddress, socketConnectTimeout);

                // Successfully connected to the server; don't try any other addresses
                return socket;
            } catch (IOException e) {
                Log.w(LOG_TAG, "could not connect to " + address, e);
                connectException = e;
            }
        }

        throw new MessagingException("Cannot connect to host", connectException);
    }

    private void startTLS() throws IOException, MessagingException, GeneralSecurityException {
        executeSimpleCommand("STARTTLS");

        String host = settings.getHost();
        int port = settings.getPort();
        String clientCertificateAlias = settings.getClientCertificateAlias();

        socket = socketFactory.createSocket(socket, host, port, clientCertificateAlias);
        socket.setSoTimeout(socketReadTimeout);

        inputStream = new PeekableInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
        responseParser = new ImapResponseParser(inputStream);
        outputStream = new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE);

        // Per RFC 2595 (3.1):  Once TLS has been started, reissue CAPABILITY command
        if (K9MailLib.isDebug()) {
            Log.i(LOG_TAG, "Updating capabilities after STARTTLS for " + getLogId());
        }

        capabilities.clear();
        List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
        if (responses.size() != 2) {
            throw new MessagingException("Invalid CAPABILITY response received");
        }
    }

    private List<ImapResponse> receiveCapabilities(List<ImapResponse> responses) {
        Set<String> receivedCapabilities = ImapResponseParser.parseCapabilities(responses);

        /* RFC 3501 6.2.3
            A server MAY include a CAPABILITY response code in the tagged OK
            response to a successful LOGIN command in order to send
            capabilities automatically.  It is unnecessary for a client to
            send a separate CAPABILITY command if it recognizes these
            automatic capabilities.
        */
        if (K9MailLib.isDebug()) {
            Log.d(LOG_TAG, "Saving " + receivedCapabilities + " capabilities for " + getLogId());
        }

        capabilities.addAll(receivedCapabilities);

        return responses;
    }

    private void authenticate(AuthType authType) throws MessagingException, IOException {
        switch (authType) {
            case CRAM_MD5: {
                if (hasCapability(CAPABILITY_AUTH_CRAM_MD5)) {
                    authCramMD5();
                } else {
                    throw new MessagingException("Server doesn't support encrypted passwords using CRAM-MD5.");
                }
                break;
            }
            case PLAIN: {
                if (hasCapability(CAPABILITY_AUTH_PLAIN)) {
                    saslAuthPlainWithLoginFallback();
                } else if (!hasCapability(CAPABILITY_LOGINDISABLED)) {
                    login();
                } else {
                    throw new MessagingException("Server doesn't support unencrypted passwords using AUTH=PLAIN " +
                            "and LOGIN is disabled.");
                }
                break;
            }
            case EXTERNAL: {
                if (hasCapability(CAPABILITY_AUTH_EXTERNAL)) {
                    saslAuthExternal();
                } else {
                    // Provide notification to user of a problem authenticating using client certificates
                    throw new CertificateValidationException(CertificateValidationException.Reason.MissingCapability);
                }
                break;
            }
            default: {
                throw new MessagingException("Unhandled authentication method found in the server settings (bug).");
            }
        }
    }

    private void authCramMD5() throws MessagingException, IOException {
        String command = "AUTHENTICATE CRAM-MD5";
        String tag = sendCommand(command, false);

        ImapResponse response = readContinuationResponse(tag);
        if (response.size() != 1 || !(response.get(0) instanceof String)) {
            throw new MessagingException("Invalid Cram-MD5 nonce received");
        }

        byte[] b64Nonce = response.getString(0).getBytes();
        byte[] b64CRAM = Authentication.computeCramMd5Bytes(settings.getUsername(), settings.getPassword(), b64Nonce);

        outputStream.write(b64CRAM);
        outputStream.write('\r');
        outputStream.write('\n');
        outputStream.flush();

        try {
            receiveCapabilities(responseParser.readStatusResponse(tag, command, getLogId(), null));
        } catch (MessagingException e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    private void saslAuthPlainWithLoginFallback() throws IOException, MessagingException {
        try {
            saslAuthPlain();
        } catch (AuthenticationFailedException e) {
            login();
        }
    }

    private void saslAuthPlain() throws IOException, MessagingException {
        String command = "AUTHENTICATE PLAIN";
        String tag = sendCommand(command, false);

        readContinuationResponse(tag);

        String credentials = "\000" + settings.getUsername() + "\000" + settings.getPassword();
        byte[] encodedCredentials = Base64.encodeBase64(credentials.getBytes());

        outputStream.write(encodedCredentials);
        outputStream.write('\r');
        outputStream.write('\n');
        outputStream.flush();

        try {
            receiveCapabilities(responseParser.readStatusResponse(tag, command, getLogId(), null));
        } catch (MessagingException e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    private void login() throws IOException, MessagingException {
        /*
         * Use quoted strings which permit spaces and quotes. (Using IMAP
         * string literals would be better, but some servers are broken
         * and don't parse them correctly.)
         */

        // escape double-quotes and backslash characters with a backslash
        Pattern p = Pattern.compile("[\\\\\"]");
        String replacement = "\\\\$0";
        String username = p.matcher(settings.getUsername()).replaceAll(replacement);
        String password = p.matcher(settings.getPassword()).replaceAll(replacement);

        try {
            receiveCapabilities(executeSimpleCommand(String.format("LOGIN \"%s\" \"%s\"", username, password), true));
        } catch (ImapException e) {
            throw new AuthenticationFailedException(e.getMessage());
        }
    }

    private void saslAuthExternal() throws IOException, MessagingException {
        try {
            receiveCapabilities(executeSimpleCommand(
                    String.format("AUTHENTICATE EXTERNAL %s", Base64.encode(settings.getUsername())), false));
        } catch (ImapException e) {
            /*
             * Provide notification to the user of a problem authenticating
             * using client certificates. We don't use an
             * AuthenticationFailedException because that would trigger a
             * "Username or password incorrect" notification in
             * AccountSetupCheckSettings.
             */
            throw new CertificateValidationException(e.getMessage());
        }
    }

    private boolean shouldEnableCompression() {
        boolean useCompression = true;

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null) {
            int type = networkInfo.getType();
            if (K9MailLib.isDebug()) {
                Log.d(LOG_TAG, "On network type " + type);
            }

            NetworkType networkType = NetworkType.fromConnectivityManagerType(type);
            useCompression = settings.useCompression(networkType);
        }

        if (K9MailLib.isDebug()) {
            Log.d(LOG_TAG, "useCompression " + useCompression);
        }

        return useCompression;
    }

    private void enableCompression() throws IOException, MessagingException {
        try {
            executeSimpleCommand(COMMAND_COMPRESS_DEFLATE);
        } catch (ImapException e) {
            Log.d(LOG_TAG, "Unable to negotiate compression: " + e.getMessage());
            return;
        }

        try {
            InflaterInputStream zInputStream = new InflaterInputStream(socket.getInputStream(), new Inflater(true));
            inputStream = new PeekableInputStream(new BufferedInputStream(zInputStream, BUFFER_SIZE));
            responseParser = new ImapResponseParser(inputStream);

            ZOutputStream zOutputStream = new ZOutputStream(socket.getOutputStream(), JZlib.Z_BEST_SPEED, true);
            outputStream = new BufferedOutputStream(zOutputStream, BUFFER_SIZE);
            zOutputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);

            if (K9MailLib.isDebug()) {
                Log.i(LOG_TAG, "Compression enabled for " + getLogId());
            }
        } catch (IOException e) {
            close();
            Log.e(LOG_TAG, "Error enabling compression", e);
        }
    }

    private void handleNamespace() throws IOException, MessagingException {
        List<ImapResponse> responses = executeSimpleCommand(COMMAND_NAMESPACE);

        for (ImapResponse response : responses) {
            if (equalsIgnoreCase(response.get(0), COMMAND_NAMESPACE)) {
                if (K9MailLib.isDebug()) {
                    Log.d(LOG_TAG, "Got NAMESPACE response " + response + " on " + getLogId());
                }

                Object personalNamespaces = response.get(1);
                if (personalNamespaces instanceof ImapList) {
                    if (K9MailLib.isDebug()) {
                        Log.d(LOG_TAG, "Got personal namespaces: " + personalNamespaces);
                    }

                    ImapList bracketed = (ImapList) personalNamespaces;
                    Object firstNamespace = bracketed.get(0);

                    if (firstNamespace != null && firstNamespace instanceof ImapList) {
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Got first personal namespaces: " + firstNamespace);
                        }

                        bracketed = (ImapList) firstNamespace;
                        settings.setPathPrefix(bracketed.getString(0));
                        settings.setPathDelimiter(bracketed.getString(1));
                        settings.setCombinedPrefix(null);

                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Got path '" + settings.getPathPrefix() + "' and separator '" +
                                    settings.getPathDelimiter() + "'");
                        }
                    }
                }
            }
        }
    }

    private void getPathDelimiter() throws IOException, MessagingException {
        List<ImapResponse> listResponses;
        try {
            listResponses = executeSimpleCommand("LIST \"\" \"\"");
        } catch (ImapException e) {
            Log.d(LOG_TAG, "Error getting path delimiter using LIST command", e);
            return;
        }

        for (ImapResponse response : listResponses) {
            if (isListResponse(response)) {
                String hierarchyDelimiter = response.getString(2);
                settings.setPathDelimiter(hierarchyDelimiter);
                settings.setCombinedPrefix(null);

                if (K9MailLib.isDebug()) {
                    Log.d(LOG_TAG, "Got path delimiter '" + settings.getPathDelimiter() + "' for " + getLogId());
                }

                break;
            }
        }
    }

    private boolean isListResponse(ImapResponse response) {
        boolean responseTooShort = response.size() < 4;
        if (responseTooShort) {
            return false;
        }

        boolean isListResponse = equalsIgnoreCase(response.get(0), "LIST");
        boolean hierarchyDelimiterValid = response.get(2) instanceof String;

        return isListResponse && hierarchyDelimiterValid;
    }

    protected boolean hasCapability(String capability) {
        return capabilities.contains(capability.toUpperCase(Locale.US));
    }

    protected boolean isIdleCapable() {
        if (K9MailLib.isDebug()) {
            Log.v(LOG_TAG, "Connection " + getLogId() + " has " + capabilities.size() + " capabilities");
        }

        return capabilities.contains(CAPABILITY_IDLE);
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public void close() {
        IOUtils.closeQuietly(inputStream);
        IOUtils.closeQuietly(outputStream);
        IOUtils.closeQuietly(socket);

        inputStream = null;
        outputStream = null;
        socket = null;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    protected String getLogId() {
        return "conn" + hashCode();
    }

    public List<ImapResponse> executeSimpleCommand(String command) throws IOException, MessagingException {
        return executeSimpleCommand(command, false, null);
    }

    public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive) throws IOException,
            MessagingException {
        return executeSimpleCommand(command, sensitive, null);
    }

    public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive, UntaggedHandler untaggedHandler)
            throws IOException, MessagingException {
        String commandToLog = command;

        if (sensitive && !K9MailLib.isDebugSensitive()) {
            commandToLog = "*sensitive*";
        }

        String tag = sendCommand(command, sensitive);

        try {
            return responseParser.readStatusResponse(tag, commandToLog, getLogId(), untaggedHandler);
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    public String sendCommand(String command, boolean sensitive) throws MessagingException, IOException {
        try {
            open();

            String tag = Integer.toString(nextCommandTag++);
            String commandToSend = tag + " " + command + "\r\n";
            outputStream.write(commandToSend.getBytes());
            outputStream.flush();

            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                if (sensitive && !K9MailLib.isDebugSensitive()) {
                    Log.v(LOG_TAG, getLogId() + ">>> [Command Hidden, Enable Sensitive Debug Logging To Show]");
                } else {
                    Log.v(LOG_TAG, getLogId() + ">>> " + commandToSend);
                }
            }

            return tag;
        } catch (IOException | MessagingException e) {
            close();
            throw e;
        }
    }

    public void sendContinuation(String continuation) throws IOException {
        outputStream.write(continuation.getBytes());
        outputStream.write('\r');
        outputStream.write('\n');
        outputStream.flush();

        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
            Log.v(LOG_TAG, getLogId() + ">>> " + continuation);
        }
    }

    public ImapResponse readResponse() throws IOException, MessagingException {
        return readResponse(null);
    }

    public ImapResponse readResponse(ImapResponseCallback callback) throws IOException {
        try {
            ImapResponse response = responseParser.readResponse(callback);

            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_IMAP) {
                Log.v(LOG_TAG, getLogId() + "<<<" + response);
            }

            return response;
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    protected void setReadTimeout(int millis) throws SocketException {
        Socket sock = socket;
        if (sock != null) {
            sock.setSoTimeout(millis);
        }
    }

    private ImapResponse readContinuationResponse(String tag) throws IOException, MessagingException {
        ImapResponse response;
        do {
            response = readResponse();

            String responseTag = response.getTag();
            if (responseTag != null) {
                if (responseTag.equalsIgnoreCase(tag)) {
                    throw new MessagingException("Command continuation aborted: " + response);
                } else {
                    Log.w(LOG_TAG, "After sending tag " + tag + ", got tag response from previous command " +
                            response + " for " + getLogId());
                }
            }
        } while (!response.isContinuationRequested());

        return response;
    }
}
