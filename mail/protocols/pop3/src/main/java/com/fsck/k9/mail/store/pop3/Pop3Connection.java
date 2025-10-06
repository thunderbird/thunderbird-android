package com.fsck.k9.mail.store.pop3;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.thunderbird.core.logging.legacy.Log;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.Authentication;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.K9MailLib;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.MissingCapabilityException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.ssl.CertificateChainExtractor;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLException;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_POP3;
import static com.fsck.k9.mail.NetworkTimeouts.SOCKET_CONNECT_TIMEOUT;
import static com.fsck.k9.mail.NetworkTimeouts.SOCKET_READ_TIMEOUT;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.AUTH_CRAM_MD5_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.AUTH_EXTERNAL_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.AUTH_PLAIN_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.CAPA_COMMAND;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.PASS_COMMAND;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.SASL_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.STLS_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.STLS_COMMAND;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.TOP_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.UIDL_CAPABILITY;
import static com.fsck.k9.mail.store.pop3.Pop3Commands.USER_COMMAND;


class Pop3Connection {

    private final Pop3Settings settings;
    private final TrustedSocketFactory trustedSocketFactory;
    private Socket socket;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private Pop3Capabilities capabilities;

    /**
     * This value is {@code true} if the server supports the CAPA command but doesn't advertise
     * support for the TOP command OR if the server doesn't support the CAPA command and we
     * already unsuccessfully tried to use the TOP command.
     */
    private boolean topNotAdvertised;

    Pop3Connection(Pop3Settings settings,
            TrustedSocketFactory trustedSocketFactory) {
        this.settings = settings;
        this.trustedSocketFactory = trustedSocketFactory;
    }

    void open() throws MessagingException {
        try {
            socket = connect();
            in = new BufferedInputStream(socket.getInputStream(), 1024);
            out = new BufferedOutputStream(socket.getOutputStream(), 512);

            socket.setSoTimeout(SOCKET_READ_TIMEOUT);

            if (!isOpen()) {
                throw new MessagingException("Unable to connect socket");
            }

            String serverGreeting = executeSimpleCommand(null);

            capabilities = getCapabilities();

            if (settings.getConnectionSecurity() == ConnectionSecurity.STARTTLS_REQUIRED) {
                performStartTlsUpgrade(trustedSocketFactory, settings.getHost(), settings.getPort(), settings.getClientCertificateAlias());
            }

            performAuthentication(settings.getAuthType(), serverGreeting);
        } catch (SSLException e) {
            List<X509Certificate> certificateChain = CertificateChainExtractor.extract(e);
            if (certificateChain != null) {
                throw new CertificateValidationException(certificateChain, e);
            } else {
                throw new MessagingException("Unable to connect", e);
            }
        } catch (GeneralSecurityException gse) {
            throw new MessagingException(
                    "Unable to open connection to POP server due to security error.", gse);
        } catch (IOException ioe) {
            close();
            throw new MessagingException("Unable to open connection to POP server.", ioe);
        }
    }

    private Socket connect()
            throws IOException, MessagingException, NoSuchAlgorithmException, KeyManagementException {
        InetAddress[] inetAddresses = InetAddress.getAllByName(settings.getHost());

        IOException connectException = null;
        for (InetAddress address : inetAddresses) {
            try {
                return connectToAddress(address);
            } catch (IOException e) {
                Log.w(e, "Could not connect to %s", address);
                connectException = e;
            }
        }

        throw connectException != null ? connectException : new UnknownHostException();
    }

    private Socket connectToAddress(InetAddress address)
            throws IOException, MessagingException, NoSuchAlgorithmException, KeyManagementException {
        if (K9MailLib.isDebug() && K9MailLib.DEBUG_PROTOCOL_POP3) {
            Log.d("Connecting to %s as %s", settings.getHost(), address);
        }

        InetSocketAddress socketAddress = new InetSocketAddress(address, settings.getPort());

        final Socket socket;
        if (settings.getConnectionSecurity() == ConnectionSecurity.SSL_TLS_REQUIRED) {
            socket = trustedSocketFactory.createSocket(null, settings.getHost(), settings.getPort(),
                    settings.getClientCertificateAlias());
        } else {
            socket = new Socket();
        }

        socket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);

        return socket;
    }

    /*
     * If STARTTLS is not available throws a CertificateValidationException which in K-9
     * triggers a "Certificate error" notification that takes the user to the incoming
     * server settings for review. This might be needed if the account was configured with an obsolete
     * "STARTTLS (if available)" setting.
     */
    private void performStartTlsUpgrade(TrustedSocketFactory trustedSocketFactory,
            String host, int port, String clientCertificateAlias)
            throws MessagingException, NoSuchAlgorithmException, KeyManagementException, IOException {
        if (capabilities.stls) {
            executeSimpleCommand(STLS_COMMAND);

            socket = trustedSocketFactory.createSocket(
                    socket,
                    host,
                    port,
                    clientCertificateAlias);
            socket.setSoTimeout(SOCKET_READ_TIMEOUT);
            in = new BufferedInputStream(socket.getInputStream(), 1024);
            out = new BufferedOutputStream(socket.getOutputStream(), 512);
            if (!isOpen()) {
                throw new MessagingException("Unable to connect socket");
            }
            capabilities = getCapabilities();
        } else {
            throw new MissingCapabilityException(STLS_CAPABILITY);
        }

    }

    private void performAuthentication(AuthType authType, String serverGreeting)
            throws MessagingException, IOException {
        switch (authType) {
            case PLAIN:
                if (capabilities.authPlain) {
                    authPlain();
                } else {
                    login();
                }
                break;

            case CRAM_MD5:
                if (capabilities.cramMD5) {
                    authCramMD5();
                } else {
                    authAPOP(serverGreeting);
                }
                break;

            case EXTERNAL:
                if (capabilities.external) {
                    authExternal();
                } else {
                    throw new MissingCapabilityException(SASL_CAPABILITY + " " + AUTH_EXTERNAL_CAPABILITY);
                }
                break;

            default:
                throw new MessagingException(
                        "Unhandled authentication method: "+authType+" found in the server settings (bug).");
        }

    }

    boolean isOpen() {
        return (in != null && out != null && socket != null
                && socket.isConnected() && !socket.isClosed());
    }

    private Pop3Capabilities getCapabilities() throws IOException {
        Pop3Capabilities capabilities = new Pop3Capabilities();
        try {
            executeSimpleCommand(CAPA_COMMAND);
            String response;
            while ((response = readLine()) != null) {
                if (response.equals(".")) {
                    break;
                }
                response = response.toUpperCase(Locale.US);
                if (response.equals(STLS_CAPABILITY)) {
                    capabilities.stls = true;
                } else if (response.equals(UIDL_CAPABILITY)) {
                    capabilities.uidl = true;
                } else if (response.equals(TOP_CAPABILITY)) {
                    capabilities.top = true;
                } else if (response.startsWith(SASL_CAPABILITY)) {
                    List<String> saslAuthMechanisms = Arrays.asList(response.split(" "));
                    if (saslAuthMechanisms.contains(AUTH_PLAIN_CAPABILITY)) {
                        capabilities.authPlain = true;
                    }
                    if (saslAuthMechanisms.contains(AUTH_CRAM_MD5_CAPABILITY)) {
                        capabilities.cramMD5 = true;
                    }
                    if (saslAuthMechanisms.contains(AUTH_EXTERNAL_CAPABILITY)) {
                        capabilities.external = true;
                    }
                }
            }

            if (!capabilities.top) {
                /*
                 * If the CAPA command is supported but it doesn't advertise support for the
                 * TOP command, we won't check for it manually.
                 */
                topNotAdvertised = true;
            }
        } catch (MessagingException me) {
            /*
             * The server may not support the CAPA command, so we just eat this Exception
             * and allow the empty capabilities object to be returned.
             */
        }
        return capabilities;
    }

    private void login() throws MessagingException, IOException {
        executeSimpleCommand(USER_COMMAND + " " + settings.getUsername());
        try {
            executeSimpleCommand(PASS_COMMAND + " " + settings.getPassword(), true);
        } catch (Pop3ErrorResponse e) {
            throw new AuthenticationFailedException("USER/PASS failed", e, e.getResponseText());
        }
    }

    private void authPlain() throws MessagingException, IOException {
        executeSimpleCommand("AUTH PLAIN");
        try {
            byte[] encodedBytes = Base64.encodeBase64(("\000" + settings.getUsername()
                    + "\000" + settings.getPassword()).getBytes());
            executeSimpleCommand(new String(encodedBytes), true);
        } catch (Pop3ErrorResponse e) {
            throw new AuthenticationFailedException("AUTH PLAIN failed", e, e.getResponseText());
        }
    }

    private void authAPOP(String serverGreeting) throws MessagingException, IOException {
        // regex based on RFC 2449 (3.) "Greeting"
        String timestamp = serverGreeting.replaceFirst(
                "^\\+OK *(?:\\[[^\\]]+\\])?[^<]*(<[^>]*>)?[^<]*$", "$1");
        if ("".equals(timestamp)) {
            throw new MessagingException(
                    "APOP authentication is not supported");
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new MessagingException(
                    "MD5 failure during POP3 auth APOP", e);
        }
        byte[] digest = md.digest((timestamp + settings.getPassword()).getBytes());
        String hexDigest = Hex.encodeHex(digest);
        try {
            executeSimpleCommand("APOP " + settings.getUsername() + " " + hexDigest, true);
        } catch (Pop3ErrorResponse e) {
            throw new AuthenticationFailedException("APOP failed", e, e.getResponseText());
        }
    }

    private void authCramMD5() throws MessagingException, IOException {
        String b64Nonce = executeSimpleCommand("AUTH CRAM-MD5").replace("+ ", "");

        String b64CRAM = Authentication.computeCramMd5(settings.getUsername(), settings.getPassword(), b64Nonce);
        try {
            executeSimpleCommand(b64CRAM, true);
        } catch (Pop3ErrorResponse e) {
            throw new AuthenticationFailedException("AUTH CRAM-MD5 failed", e, e.getResponseText());
        }
    }

    private void authExternal() throws MessagingException, IOException {
        try {
            executeSimpleCommand(
                    String.format("AUTH EXTERNAL %s",
                            Base64.encode(settings.getUsername())), false);
        } catch (Pop3ErrorResponse e) {
            throw new AuthenticationFailedException("AUTH EXTERNAL failed", e, e.getResponseText());
        }
    }

    private void writeLine(String s) throws IOException {
        out.write(s.getBytes());
        out.write('\r');
        out.write('\n');
        out.flush();
    }

    String executeSimpleCommand(String command) throws IOException, Pop3ErrorResponse {
        return executeSimpleCommand(command, false);
    }

    private String executeSimpleCommand(String command, boolean sensitive) throws IOException, Pop3ErrorResponse {
        if (command != null) {
            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                if (sensitive && !K9MailLib.isDebugSensitive()) {
                    Log.d(">>> [Command Hidden, Enable Sensitive Debug Logging To Show]");
                } else {
                    Log.d(">>> %s", command);
                }
            }

            writeLine(command);
        }

        String response = readLine();
        if (response.length() == 0 || response.charAt(0) != '+') {
            throw new Pop3ErrorResponse(response);
        }

        return response;
    }

    String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int d = in.read();
        if (d == -1) {
            throw new IOException("End of stream reached while trying to read line.");
        }
        do {
            if (((char)d) == '\r') {
                //noinspection UnnecessaryContinue Makes it easier to follow
                continue;
            } else if (((char)d) == '\n') {
                break;
            } else {
                sb.append((char)d);
            }
        } while ((d = in.read()) != -1);
        String ret = sb.toString();
        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
            Log.d("<<< %s", ret);
        }
        return ret;
    }

    void close() {
        try {
            in.close();
        } catch (Exception e) {
            /*
             * May fail if the connection is already closed.
             */
        }
        try {
            out.close();
        } catch (Exception e) {
            /*
             * May fail if the connection is already closed.
             */
        }
        try {
            socket.close();
        } catch (Exception e) {
            /*
             * May fail if the connection is already closed.
             */
        }
        in = null;
        out = null;
        socket = null;
    }

    boolean supportsTop() {
        return capabilities.top;
    }

    boolean isTopNotAdvertised() {
        return topNotAdvertised;
    }

    void setSupportsTop(boolean supportsTop) {
        this.capabilities.top = supportsTop;
    }

    void setTopNotAdvertised(boolean topNotAdvertised) {
        this.topNotAdvertised = topNotAdvertised;
    }

    boolean supportsUidl() {
        return this.capabilities.uidl;
    }

    InputStream getInputStream() {
        return in;
    }
}
