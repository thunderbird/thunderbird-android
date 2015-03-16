
package com.fsck.k9.mail.store.pop3;

import android.annotation.SuppressLint;
import android.util.Log;

import com.fsck.k9.mail.*;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.filter.Hex;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;

import javax.net.ssl.SSLException;

import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.fsck.k9.mail.K9MailLib.DEBUG_PROTOCOL_POP3;
import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.CertificateValidationException.Reason.MissingCapability;

public class Pop3Store extends RemoteStore {

    private static final String STLS_COMMAND = "STLS";
    private static final String USER_COMMAND = "USER";
    private static final String PASS_COMMAND = "PASS";
    private static final String CAPA_COMMAND = "CAPA";
    private static final String AUTH_COMMAND = "AUTH";
    private static final String STAT_COMMAND = "STAT";
    private static final String LIST_COMMAND = "LIST";
    private static final String UIDL_COMMAND = "UIDL";
    private static final String TOP_COMMAND = "TOP";
    private static final String RETR_COMMAND = "RETR";
    private static final String DELE_COMMAND = "DELE";
    private static final String QUIT_COMMAND = "QUIT";

    private static final String STLS_CAPABILITY = "STLS";
    private static final String UIDL_CAPABILITY = "UIDL";
    private static final String TOP_CAPABILITY = "TOP";
    private static final String SASL_CAPABILITY = "SASL";
    private static final String AUTH_PLAIN_CAPABILITY = "PLAIN";
    private static final String AUTH_CRAM_MD5_CAPABILITY = "CRAM-MD5";
    private static final String AUTH_EXTERNAL_CAPABILITY = "EXTERNAL";

    /**
     * Decodes a Pop3Store URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * pop3://auth:user:password@server:port ConnectionSecurity.NONE
     * pop3+tls+://auth:user:password@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * pop3+ssl+://auth:user:password@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static ServerSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;

        URI pop3Uri;
        try {
            pop3Uri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid Pop3Store URI", use);
        }

        String scheme = pop3Uri.getScheme();
        /*
         * Currently available schemes are:
         * pop3
         * pop3+tls+
         * pop3+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * pop3+tls
         * pop3+ssl
         */
        if (scheme.equals("pop3")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = Type.POP3.defaultPort;
        } else if (scheme.startsWith("pop3+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = Type.POP3.defaultPort;
        } else if (scheme.startsWith("pop3+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = Type.POP3.defaultTlsPort;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = pop3Uri.getHost();

        if (pop3Uri.getPort() != -1) {
            port = pop3Uri.getPort();
        }

        AuthType authType = AuthType.PLAIN;
        if (pop3Uri.getUserInfo() != null) {
            int userIndex = 0, passwordIndex = 1;
            String userinfo = pop3Uri.getUserInfo();
            String[] userInfoParts = userinfo.split(":");
            if (userInfoParts.length > 2 || userinfo.endsWith(":") ) {
                // If 'userinfo' ends with ":" the password is empty. This can only happen
                // after an account was imported (so authType and username are present).
                userIndex++;
                passwordIndex++;
                authType = AuthType.valueOf(userInfoParts[0]);
            }
            username = decodeUtf8(userInfoParts[userIndex]);
            if (userInfoParts.length > passwordIndex) {
                if (authType == AuthType.EXTERNAL) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[passwordIndex]);
                } else {
                    password = decodeUtf8(userInfoParts[passwordIndex]);
                }
            }
        }

        return new ServerSettings(ServerSettings.Type.POP3, host, port, connectionSecurity, authType, username,
                password, clientCertificateAlias);
    }

    /**
     * Creates a Pop3Store URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A Pop3Store URI that holds the same information as the {@code server} parameter.
     *
     * @see StoreConfig#getStoreUri()
     * @see Pop3Store#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ?
                    encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                    encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "pop3+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "pop3+tls+";
                break;
            default:
            case NONE:
                scheme = "pop3";
                break;
        }

        AuthType authType = server.authenticationType;
        String userInfo;
        if (AuthType.EXTERNAL == authType) {
            userInfo = authType.name() + ":" + userEnc + ":" + clientCertificateAliasEnc;
        } else {
            userInfo = authType.name() + ":" + userEnc + ":" + passwordEnc;
        }

        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create Pop3Store URI", e);
        }
    }


    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mClientCertificateAlias;
    private AuthType mAuthType;
    private ConnectionSecurity mConnectionSecurity;
    private Map<String, Folder> mFolders = new HashMap<String, Folder>();
    private Pop3Capabilities mCapabilities;

    /**
     * This value is {@code true} if the server supports the CAPA command but doesn't advertise
     * support for the TOP command OR if the server doesn't support the CAPA command and we
     * already unsuccessfully tried to use the TOP command.
     */
    private boolean mTopNotSupported;


    public Pop3Store(StoreConfig storeConfig, TrustedSocketFactory socketFactory) throws MessagingException {
        super(storeConfig, socketFactory);

        ServerSettings settings;
        try {
            settings = decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        mConnectionSecurity = settings.connectionSecurity;

        mUsername = settings.username;
        mPassword = settings.password;
        mClientCertificateAlias = settings.clientCertificateAlias;
        mAuthType = settings.authenticationType;
    }

    @Override
    public Folder getFolder(String name) {
        Folder folder = mFolders.get(name);
        if (folder == null) {
            folder = new Pop3Folder(name);
            mFolders.put(folder.getName(), folder);
        }
        return folder;
    }

    @Override
    public List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        List<Folder> folders = new LinkedList<Folder>();
        folders.add(getFolder(mStoreConfig.getInboxFolderName()));
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mStoreConfig.getInboxFolderName());
        folder.open(Folder.OPEN_MODE_RW);
        if (!mCapabilities.uidl) {
            /*
             * Run an additional test to see if UIDL is supported on the server. If it's not we
             * can't service this account.
             */

            /*
             * If the server doesn't support UIDL it will return a - response, which causes
             * executeSimpleCommand to throw a MessagingException, exiting this method.
             */
            folder.executeSimpleCommand(UIDL_COMMAND);

        }
        folder.close();
    }

    @Override
    public boolean isSeenFlagSupported() {
        return false;
    }

    class Pop3Folder extends Folder<Pop3Message> {
        private Socket mSocket;
        private InputStream mIn;
        private OutputStream mOut;
        private Map<String, Pop3Message> mUidToMsgMap = new HashMap<String, Pop3Message>();
        @SuppressLint("UseSparseArrays")
        private Map<Integer, Pop3Message> mMsgNumToMsgMap = new HashMap<Integer, Pop3Message>();
        private Map<String, Integer> mUidToMsgNumMap = new HashMap<String, Integer>();
        private String mName;
        private int mMessageCount;

        public Pop3Folder(String name) {
            super();
            this.mName = name;

            if (mName.equalsIgnoreCase(mStoreConfig.getInboxFolderName())) {
                mName = mStoreConfig.getInboxFolderName();
            }
        }

        @Override
        public synchronized void open(int mode) throws MessagingException {
            if (isOpen()) {
                return;
            }

            if (!mName.equalsIgnoreCase(mStoreConfig.getInboxFolderName())) {
                throw new MessagingException("Folder does not exist");
            }

            try {
                SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
                if (mConnectionSecurity == ConnectionSecurity.SSL_TLS_REQUIRED) {
                    mSocket = mTrustedSocketFactory.createSocket(null, mHost, mPort, mClientCertificateAlias);
                } else {
                    mSocket = new Socket();
                }

                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);

                mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
                if (!isOpen()) {
                    throw new MessagingException("Unable to connect socket");
                }

                String serverGreeting = executeSimpleCommand(null);

                mCapabilities = getCapabilities();
                if (mConnectionSecurity == ConnectionSecurity.STARTTLS_REQUIRED) {

                    if (mCapabilities.stls) {
                        executeSimpleCommand(STLS_COMMAND);

                        mSocket = mTrustedSocketFactory.createSocket(
                                mSocket,
                                mHost,
                                mPort,
                                mClientCertificateAlias);
                        mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);
                        mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                        mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
                        if (!isOpen()) {
                            throw new MessagingException("Unable to connect socket");
                        }
                        mCapabilities = getCapabilities();
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

                switch (mAuthType) {
                case PLAIN:
                    if (mCapabilities.authPlain) {
                        authPlain();
                    } else {
                        login();
                    }
                    break;

                case CRAM_MD5:
                    if (mCapabilities.cramMD5) {
                        authCramMD5();
                    } else {
                        authAPOP(serverGreeting);
                    }
                    break;

                case EXTERNAL:
                    if (mCapabilities.external) {
                        authExternal();
                    } else {
                        // Provide notification to user of a problem authenticating using client certificates
                        throw new CertificateValidationException(MissingCapability);
                    }
                    break;

                default:
                    throw new MessagingException(
                            "Unhandled authentication method found in the server settings (bug).");
                }
            } catch (SSLException e) {
                throw new CertificateValidationException(e.getMessage(), e);
            } catch (GeneralSecurityException gse) {
                throw new MessagingException(
                    "Unable to open connection to POP server due to security error.", gse);
            } catch (IOException ioe) {
                throw new MessagingException("Unable to open connection to POP server.", ioe);
            }

            String response = executeSimpleCommand(STAT_COMMAND);
            String[] parts = response.split(" ");
            mMessageCount = Integer.parseInt(parts[1]);

            mUidToMsgMap.clear();
            mMsgNumToMsgMap.clear();
            mUidToMsgNumMap.clear();
        }

        private void login() throws MessagingException {
            executeSimpleCommand(USER_COMMAND + " " + mUsername);
            try {
                executeSimpleCommand(PASS_COMMAND + " " + mPassword, true);
            } catch (Pop3ErrorResponse e) {
                throw new AuthenticationFailedException(
                        "POP3 login authentication failed: " + e.getMessage(), e);
            }
        }

        private void authPlain() throws MessagingException {
            executeSimpleCommand("AUTH PLAIN");
            try {
                byte[] encodedBytes = Base64.encodeBase64(("\000" + mUsername
                        + "\000" + mPassword).getBytes());
                executeSimpleCommand(new String(encodedBytes), true);
            } catch (Pop3ErrorResponse e) {
                throw new AuthenticationFailedException(
                        "POP3 SASL auth PLAIN authentication failed: "
                                + e.getMessage(), e);
          }
        }

        private void authAPOP(String serverGreeting) throws MessagingException {
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
            byte[] digest = md.digest((timestamp + mPassword).getBytes());
            String hexDigest = new String(Hex.encodeHex(digest));
            try {
                executeSimpleCommand("APOP " + mUsername + " " + hexDigest, true);
            } catch (Pop3ErrorResponse e) {
                throw new AuthenticationFailedException(
                        "POP3 APOP authentication failed: " + e.getMessage(), e);
            }
        }

        private void authCramMD5() throws MessagingException {
            String b64Nonce = executeSimpleCommand("AUTH CRAM-MD5").replace("+ ", "");

            String b64CRAM = Authentication.computeCramMd5(mUsername, mPassword, b64Nonce);
            try {
                executeSimpleCommand(b64CRAM, true);
            } catch (Pop3ErrorResponse e) {
                throw new AuthenticationFailedException(
                        "POP3 CRAM-MD5 authentication failed: "
                                + e.getMessage(), e);
            }
        }

        private void authExternal() throws MessagingException {
            try {
                executeSimpleCommand(
                        String.format("AUTH EXTERNAL %s",
                                Base64.encode(mUsername)), false);
            } catch (Pop3ErrorResponse e) {
                /*
                 * Provide notification to the user of a problem authenticating
                 * using client certificates. We don't use an
                 * AuthenticationFailedException because that would trigger a
                 * "Username or password incorrect" notification in
                 * AccountSetupCheckSettings.
                 */
                throw new CertificateValidationException(
                        "POP3 client certificate authentication failed: " + e.getMessage(), e);
            }
        }

        @Override
        public boolean isOpen() {
            return (mIn != null && mOut != null && mSocket != null
                    && mSocket.isConnected() && !mSocket.isClosed());
        }

        @Override
        public int getMode() {
            return Folder.OPEN_MODE_RW;
        }

        @Override
        public void close() {
            try {
                if (isOpen()) {
                    executeSimpleCommand(QUIT_COMMAND);
                }
            } catch (Exception e) {
                /*
                 * QUIT may fail if the connection is already closed. We don't care. It's just
                 * being friendly.
                 */
            }

            closeIO();
        }

        private void closeIO() {
            try {
                mIn.close();
            } catch (Exception e) {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            try {
                mOut.close();
            } catch (Exception e) {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            try {
                mSocket.close();
            } catch (Exception e) {
                /*
                 * May fail if the connection is already closed.
                 */
            }
            mIn = null;
            mOut = null;
            mSocket = null;
        }

        @Override
        public String getName() {
            return mName;
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            return false;
        }

        @Override
        public boolean exists() throws MessagingException {
            return mName.equalsIgnoreCase(mStoreConfig.getInboxFolderName());
        }

        @Override
        public int getMessageCount() {
            return mMessageCount;
        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            return -1;
        }
        @Override
        public int getFlaggedMessageCount() throws MessagingException {
            return -1;
        }

        @Override
        public Pop3Message getMessage(String uid) throws MessagingException {
            Pop3Message message = mUidToMsgMap.get(uid);
            if (message == null) {
                message = new Pop3Message(uid, this);
            }
            return message;
        }

        @Override
        public List<Pop3Message> getMessages(int start, int end, Date earliestDate, MessageRetrievalListener<Pop3Message> listener)
        throws MessagingException {
            if (start < 1 || end < 1 || end < start) {
                throw new MessagingException(String.format(Locale.US, "Invalid message set %d %d",
                                             start, end));
            }
            try {
                indexMsgNums(start, end);
            } catch (IOException ioe) {
                throw new MessagingException("getMessages", ioe);
            }
            List<Pop3Message> messages = new ArrayList<Pop3Message>();
            int i = 0;
            for (int msgNum = start; msgNum <= end; msgNum++) {
                Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                if (message == null) {
                    /*
                     * There could be gaps in the message numbers or malformed
                     * responses which lead to "gaps" in mMsgNumToMsgMap.
                     *
                     * See issue 2252
                     */
                    continue;
                }

                if (listener != null) {
                    listener.messageStarted(message.getUid(), i++, (end - start) + 1);
                }
                messages.add(message);
                if (listener != null) {
                    listener.messageFinished(message, i++, (end - start) + 1);
                }
            }
            return messages;
        }

        /**
         * Ensures that the given message set (from start to end inclusive)
         * has been queried so that uids are available in the local cache.
         * @param start
         * @param end
         * @throws MessagingException
         * @throws IOException
         */
        private void indexMsgNums(int start, int end)
        throws MessagingException, IOException {
            int unindexedMessageCount = 0;
            for (int msgNum = start; msgNum <= end; msgNum++) {
                if (mMsgNumToMsgMap.get(msgNum) == null) {
                    unindexedMessageCount++;
                }
            }
            if (unindexedMessageCount == 0) {
                return;
            }
            if (unindexedMessageCount < 50 && mMessageCount > 5000) {
                /*
                 * In extreme cases we'll do a UIDL command per message instead of a bulk
                 * download.
                 */
                for (int msgNum = start; msgNum <= end; msgNum++) {
                    Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                    if (message == null) {
                        String response = executeSimpleCommand(UIDL_COMMAND + " " + msgNum);
                        // response = "+OK msgNum msgUid"
                        String[] uidParts = response.split(" +");
                        if (uidParts.length < 3 || !"+OK".equals(uidParts[0])) {
                            Log.e(LOG_TAG, "ERR response: " + response);
                            return;
                        }
                        String msgUid = uidParts[2];
                        message = new Pop3Message(msgUid, this);
                        indexMessage(msgNum, message);
                    }
                }
            } else {
                String response = executeSimpleCommand(UIDL_COMMAND);
                while ((response = readLine()) != null) {
                    if (response.equals(".")) {
                        break;
                    }

                    /*
                     * Yet another work-around for buggy server software:
                     * split the response into message number and unique identifier, no matter how many spaces it has
                     *
                     * Example for a malformed response:
                     * 1   2011071307115510400ae3e9e00bmu9
                     *
                     * Note the three spaces between message number and unique identifier.
                     * See issue 3546
                     */

                    String[] uidParts = response.split(" +");
                    if ((uidParts.length >= 3) && "+OK".equals(uidParts[0])) {
                        /*
                         * At least one server software places a "+OK" in
                         * front of every line in the unique-id listing.
                         *
                         * Fix up the array if we detected this behavior.
                         * See Issue 1237
                         */
                        uidParts[0] = uidParts[1];
                        uidParts[1] = uidParts[2];
                    }
                    if (uidParts.length >= 2) {
                        Integer msgNum = Integer.valueOf(uidParts[0]);
                        String msgUid = uidParts[1];
                        if (msgNum >= start && msgNum <= end) {
                            Pop3Message message = mMsgNumToMsgMap.get(msgNum);
                            if (message == null) {
                                message = new Pop3Message(msgUid, this);
                                indexMessage(msgNum, message);
                            }
                        }
                    }
                }
            }
        }

        private void indexUids(List<String> uids)
        throws MessagingException, IOException {
            Set<String> unindexedUids = new HashSet<String>();
            for (String uid : uids) {
                if (mUidToMsgMap.get(uid) == null) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                        Log.d(LOG_TAG, "Need to index UID " + uid);
                    }
                    unindexedUids.add(uid);
                }
            }
            if (unindexedUids.isEmpty()) {
                return;
            }
            /*
             * If we are missing uids in the cache the only sure way to
             * get them is to do a full UIDL list. A possible optimization
             * would be trying UIDL for the latest X messages and praying.
             */
            String response = executeSimpleCommand(UIDL_COMMAND);
            while ((response = readLine()) != null) {
                if (response.equals(".")) {
                    break;
                }
                String[] uidParts = response.split(" +");

                // Ignore messages without a unique-id
                if (uidParts.length >= 2) {
                    Integer msgNum = Integer.valueOf(uidParts[0]);
                    String msgUid = uidParts[1];
                    if (unindexedUids.contains(msgUid)) {
                        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                            Log.d(LOG_TAG, "Got msgNum " + msgNum + " for UID " + msgUid);
                        }

                        Pop3Message message = mUidToMsgMap.get(msgUid);
                        if (message == null) {
                            message = new Pop3Message(msgUid, this);
                        }
                        indexMessage(msgNum, message);
                    }
                }
            }
        }

        private void indexMessage(int msgNum, Pop3Message message) {
            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                Log.d(LOG_TAG, "Adding index for UID " + message.getUid() + " to msgNum " + msgNum);
            }
            mMsgNumToMsgMap.put(msgNum, message);
            mUidToMsgMap.put(message.getUid(), message);
            mUidToMsgNumMap.put(message.getUid(), msgNum);
        }

        @Override
        public List<Pop3Message> getMessages(MessageRetrievalListener listener) throws MessagingException {
            throw new UnsupportedOperationException("Pop3: No getMessages");
        }

        @Override
        public List<Pop3Message> getMessages(String[] uids, MessageRetrievalListener listener)
        throws MessagingException {
            throw new UnsupportedOperationException("Pop3: No getMessages by uids");
        }

        /**
         * Fetch the items contained in the FetchProfile into the given set of
         * Messages in as efficient a manner as possible.
         * @param messages
         * @param fp
         * @throws MessagingException
         */
        @Override
        public void fetch(List<Pop3Message> messages, FetchProfile fp, MessageRetrievalListener<Pop3Message> listener)
        throws MessagingException {
            if (messages == null || messages.isEmpty()) {
                return;
            }
            List<String> uids = new ArrayList<String>();
            for (Message message : messages) {
                uids.add(message.getUid());
            }
            try {
                indexUids(uids);
            } catch (IOException ioe) {
                throw new MessagingException("fetch", ioe);
            }
            try {
                if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                    /*
                     * We pass the listener only if there are other things to do in the
                     * FetchProfile. Since fetchEnvelop works in bulk and eveything else
                     * works one at a time if we let fetchEnvelope send events the
                     * event would get sent twice.
                     */
                    fetchEnvelope(messages, fp.size() == 1 ? listener : null);
                }
            } catch (IOException ioe) {
                throw new MessagingException("fetch", ioe);
            }
            for (int i = 0, count = messages.size(); i < count; i++) {
                Pop3Message pop3Message = messages.get(i);
                try {
                    if (listener != null && !fp.contains(FetchProfile.Item.ENVELOPE)) {
                        listener.messageStarted(pop3Message.getUid(), i, count);
                    }
                    if (fp.contains(FetchProfile.Item.BODY)) {
                        fetchBody(pop3Message, -1);
                    } else if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                        /*
                         * To convert the suggested download size we take the size
                         * divided by the maximum line size (76).
                         */
                        if (mStoreConfig.getMaximumAutoDownloadMessageSize() > 0) {
                            fetchBody(pop3Message,
                                      (mStoreConfig.getMaximumAutoDownloadMessageSize() / 76));
                        } else {
                            fetchBody(pop3Message, -1);
                        }
                    } else if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                        /*
                         * If the user is requesting STRUCTURE we are required to set the body
                         * to null since we do not support the function.
                         */
                        pop3Message.setBody(null);
                    }
                    if (listener != null && !(fp.contains(FetchProfile.Item.ENVELOPE) && fp.size() == 1)) {
                        listener.messageFinished(pop3Message, i, count);
                    }
                } catch (IOException ioe) {
                    throw new MessagingException("Unable to fetch message", ioe);
                }
            }
        }

        private void fetchEnvelope(List<Pop3Message> messages,
                                   MessageRetrievalListener<Pop3Message> listener)  throws IOException, MessagingException {
            int unsizedMessages = 0;
            for (Message message : messages) {
                if (message.getSize() == -1) {
                    unsizedMessages++;
                }
            }
            if (unsizedMessages == 0) {
                return;
            }
            if (unsizedMessages < 50 && mMessageCount > 5000) {
                /*
                 * In extreme cases we'll do a command per message instead of a bulk request
                 * to hopefully save some time and bandwidth.
                 */
                for (int i = 0, count = messages.size(); i < count; i++) {
                    Pop3Message message = messages.get(i);
                    if (listener != null) {
                        listener.messageStarted(message.getUid(), i, count);
                    }
                    String response = executeSimpleCommand(String.format(Locale.US, LIST_COMMAND + " %d",
                                                           mUidToMsgNumMap.get(message.getUid())));
                    String[] listParts = response.split(" ");
                    //int msgNum = Integer.parseInt(listParts[1]);
                    int msgSize = Integer.parseInt(listParts[2]);
                    message.setSize(msgSize);
                    if (listener != null) {
                        listener.messageFinished(message, i, count);
                    }
                }
            } else {
                Set<String> msgUidIndex = new HashSet<String>();
                for (Message message : messages) {
                    msgUidIndex.add(message.getUid());
                }
                int i = 0, count = messages.size();
                String response = executeSimpleCommand(LIST_COMMAND);
                while ((response = readLine()) != null) {
                    if (response.equals(".")) {
                        break;
                    }
                    String[] listParts = response.split(" ");
                    int msgNum = Integer.parseInt(listParts[0]);
                    int msgSize = Integer.parseInt(listParts[1]);
                    Pop3Message pop3Message = mMsgNumToMsgMap.get(msgNum);
                    if (pop3Message != null && msgUidIndex.contains(pop3Message.getUid())) {
                        if (listener != null) {
                            listener.messageStarted(pop3Message.getUid(), i, count);
                        }
                        pop3Message.setSize(msgSize);
                        if (listener != null) {
                            listener.messageFinished(pop3Message, i, count);
                        }
                        i++;
                    }
                }
            }
        }

        /**
         * Fetches the body of the given message, limiting the downloaded data to the specified
         * number of lines if possible.
         *
         * If lines is -1 the entire message is fetched. This is implemented with RETR for
         * lines = -1 or TOP for any other value. If the server does not support TOP, RETR is used
         * instead.
         */
        private void fetchBody(Pop3Message message, int lines)
        throws IOException, MessagingException {
            String response = null;

            // Try hard to use the TOP command if we're not asked to download the whole message.
            if (lines != -1 && (!mTopNotSupported || mCapabilities.top)) {
                try {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3 && !mCapabilities.top) {
                        Log.d(LOG_TAG, "This server doesn't support the CAPA command. " +
                              "Checking to see if the TOP command is supported nevertheless.");
                    }

                    response = executeSimpleCommand(String.format(Locale.US, TOP_COMMAND + " %d %d",
                                                    mUidToMsgNumMap.get(message.getUid()), lines));

                    // TOP command is supported. Remember this for the next time.
                    mCapabilities.top = true;
                } catch (Pop3ErrorResponse e) {
                    if (mCapabilities.top) {
                        // The TOP command should be supported but something went wrong.
                        throw e;
                    } else {
                        if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                            Log.d(LOG_TAG, "The server really doesn't support the TOP " +
                                  "command. Using RETR instead.");
                        }

                        // Don't try to use the TOP command again.
                        mTopNotSupported = true;
                    }
                }
            }

            if (response == null) {
                executeSimpleCommand(String.format(Locale.US, RETR_COMMAND + " %d",
                                     mUidToMsgNumMap.get(message.getUid())));
            }

            try {
                message.parse(new Pop3ResponseInputStream(mIn));

                // TODO: if we've received fewer lines than requested we also have the complete message.
                if (lines == -1 || !mCapabilities.top) {
                    message.setFlag(Flag.X_DOWNLOADED_FULL, true);
                }
            } catch (MessagingException me) {
                /*
                 * If we're only downloading headers it's possible
                 * we'll get a broken MIME message which we're not
                 * real worried about. If we've downloaded the body
                 * and can't parse it we need to let the user know.
                 */
                if (lines == -1) {
                    throw me;
                }
            }
        }

        @Override
        public Map<String, String> appendMessages(List<? extends Message> messages) throws MessagingException {
            return null;
        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
        }

        @Override
        public void delete(List<? extends Message> msgs, String trashFolderName) throws MessagingException {
            setFlags(msgs, Collections.singleton(Flag.DELETED), true);
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            return null;
        }

        @Override
        public void setFlags(final Set<Flag> flags, boolean value) throws MessagingException {
            throw new UnsupportedOperationException("POP3: No setFlags(Set<Flag>,boolean)");
        }

        @Override
        public void setFlags(List<? extends Message> messages, final Set<Flag> flags, boolean value)
        throws MessagingException {
            if (!value || !flags.contains(Flag.DELETED)) {
                /*
                 * The only flagging we support is setting the Deleted flag.
                 */
                return;
            }
            List<String> uids = new ArrayList<String>();
            try {
                for (Message message : messages) {
                    uids.add(message.getUid());
                }

                indexUids(uids);
            } catch (IOException ioe) {
                throw new MessagingException("Could not get message number for uid " + uids, ioe);
            }
            for (Message message : messages) {

                Integer msgNum = mUidToMsgNumMap.get(message.getUid());
                if (msgNum == null) {
                    MessagingException me = new MessagingException("Could not delete message " + message.getUid()
                            + " because no msgNum found; permanent error");
                    me.setPermanentFailure(true);
                    throw me;
                }
                executeSimpleCommand(String.format(DELE_COMMAND + " %s", msgNum));
            }
        }

        private String readLine() throws IOException {
            StringBuilder sb = new StringBuilder();
            int d = mIn.read();
            if (d == -1) {
                throw new IOException("End of stream reached while trying to read line.");
            }
            do {
                if (((char)d) == '\r') {
                    continue;
                } else if (((char)d) == '\n') {
                    break;
                } else {
                    sb.append((char)d);
                }
            } while ((d = mIn.read()) != -1);
            String ret = sb.toString();
            if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                Log.d(LOG_TAG, "<<< " + ret);
            }
            return ret;
        }

        private void writeLine(String s) throws IOException {
            mOut.write(s.getBytes());
            mOut.write('\r');
            mOut.write('\n');
            mOut.flush();
        }

        private Pop3Capabilities getCapabilities() throws IOException {
            Pop3Capabilities capabilities = new Pop3Capabilities();
            try {
                /*
                 * Try sending an AUTH command with no arguments.
                 *
                 * The server may respond with a list of supported SASL
                 * authentication mechanisms.
                 *
                 * Ref.: http://tools.ietf.org/html/draft-myers-sasl-pop3-05
                 *
                 * While this never became a standard, there are servers that
                 * support it, and Thunderbird includes this check.
                 */
                String response = executeSimpleCommand(AUTH_COMMAND);
                while ((response = readLine()) != null) {
                    if (response.equals(".")) {
                        break;
                    }
                    response = response.toUpperCase(Locale.US);
                    if (response.equals(AUTH_PLAIN_CAPABILITY)) {
                        capabilities.authPlain = true;
                    } else if (response.equals(AUTH_CRAM_MD5_CAPABILITY)) {
                        capabilities.cramMD5 = true;
                    } else if (response.equals(AUTH_EXTERNAL_CAPABILITY)) {
                        capabilities.external = true;
                    }
                }
            } catch (MessagingException e) {
                // Assume AUTH command with no arguments is not supported.
            }
            try {
                String response = executeSimpleCommand(CAPA_COMMAND);
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
                    }
                }

                if (!capabilities.top) {
                    /*
                     * If the CAPA command is supported but it doesn't advertise support for the
                     * TOP command, we won't check for it manually.
                     */
                    mTopNotSupported = true;
                }
            } catch (MessagingException me) {
                /*
                 * The server may not support the CAPA command, so we just eat this Exception
                 * and allow the empty capabilities object to be returned.
                 */
            }
            return capabilities;
        }

        private String executeSimpleCommand(String command) throws MessagingException {
            return executeSimpleCommand(command, false);
        }

        private String executeSimpleCommand(String command, boolean sensitive) throws MessagingException {
            try {
                open(Folder.OPEN_MODE_RW);

                if (command != null) {
                    if (K9MailLib.isDebug() && DEBUG_PROTOCOL_POP3) {
                        if (sensitive && !K9MailLib.isDebugSensitive()) {
                            Log.d(LOG_TAG, ">>> "
                                  + "[Command Hidden, Enable Sensitive Debug Logging To Show]");
                        } else {
                            Log.d(LOG_TAG, ">>> " + command);
                        }
                    }

                    writeLine(command);
                }

                String response = readLine();
                if (response.length() == 0 || response.charAt(0) != '+') {
                    throw new Pop3ErrorResponse(response);
                }

                return response;
            } catch (MessagingException me) {
                throw me;
            } catch (Exception e) {
                closeIO();
                throw new MessagingException("Unable to execute POP3 command", e);
            }
        }

        @Override
        public boolean isFlagSupported(Flag flag) {
            return (flag == Flag.DELETED);
        }

        @Override
        public boolean supportsFetchingFlags() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Pop3Folder) {
                return ((Pop3Folder) o).mName.equals(mName);
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return mName.hashCode();
        }

    }//Pop3Folder

    static class Pop3Message extends MimeMessage {
        public Pop3Message(String uid, Pop3Folder folder) {
            mUid = uid;
            mFolder = folder;
            mSize = -1;
        }

        public void setSize(int size) {
            mSize = size;
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(Collections.singletonList(this), Collections.singleton(flag), set);
        }

        @Override
        public void delete(String trashFolderName) throws MessagingException {
            //  try
            //  {
            //  Poor POP3 users, we can't copy the message to the Trash folder, but they still want a delete
            setFlag(Flag.DELETED, true);
            //   }
//         catch (MessagingException me)
//         {
//          Log.w(LOG_TAG, "Could not delete non-existent message", me);
//         }
        }
    }

    static class Pop3Capabilities {
        public boolean cramMD5;
        public boolean authPlain;
        public boolean stls;
        public boolean top;
        public boolean uidl;
        public boolean external;

        @Override
        public String toString() {
            return String.format("CRAM-MD5 %b, PLAIN %b, STLS %b, TOP %b, UIDL %b, EXTERNAL %b",
                                 cramMD5,
                                 authPlain,
                                 stls,
                                 top,
                                 uidl,
                                 external);
        }
    }

    static class Pop3ResponseInputStream extends InputStream {
        private InputStream mIn;
        private boolean mStartOfLine = true;
        private boolean mFinished;

        public Pop3ResponseInputStream(InputStream in) {
            mIn = in;
        }

        @Override
        public int read() throws IOException {
            if (mFinished) {
                return -1;
            }
            int d = mIn.read();
            if (mStartOfLine && d == '.') {
                d = mIn.read();
                if (d == '\r') {
                    mFinished = true;
                    mIn.read();
                    return -1;
                }
            }

            mStartOfLine = (d == '\n');

            return d;
        }
    }

    /**
     * Exception that is thrown if the server returns an error response.
     */
    static class Pop3ErrorResponse extends MessagingException {
        private static final long serialVersionUID = 3672087845857867174L;

        public Pop3ErrorResponse(String message) {
            super(message, true);
        }
    }
}
