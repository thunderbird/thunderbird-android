
package com.fsck.k9.mail.store;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.controller.MessageRetrievalListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.*;

import com.fsck.k9.mail.internet.MimeMessage;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.*;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Pop3Store extends Store {
    public static final String STORE_TYPE = "POP3";

    public static final int CONNECTION_SECURITY_NONE = 0;
    public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
    public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
    public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
    public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

    private enum AuthType {
        PLAIN,
        CRAM_MD5
    }

    private static final String STLS_COMMAND = "STLS";
    private static final String USER_COMMAND = "USER";
    private static final String PASS_COMMAND = "PASS";
    private static final String CAPA_COMMAND = "CAPA";
    private static final String STAT_COMMAND = "STAT";
    private static final String LIST_COMMAND = "LIST";
    private static final String UIDL_COMMAND = "UIDL";
    private static final String TOP_COMMAND = "TOP";
    private static final String RETR_COMMAND = "RETR";
    private static final String DELE_COMMAND = "DELE";
    private static final String QUIT_COMMAND = "QUIT";

    private static final String STLS_CAPABILITY = "STLS";
    private static final String UIDL_CAPABILITY = "UIDL";
    private static final String PIPELINING_CAPABILITY = "PIPELINING";
    private static final String USER_CAPABILITY = "USER";
    private static final String TOP_CAPABILITY = "TOP";

    /**
     * Decodes a Pop3Store URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * pop3://user:password@server:port CONNECTION_SECURITY_NONE
     * pop3+tls://user:password@server:port CONNECTION_SECURITY_TLS_OPTIONAL
     * pop3+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
     * pop3+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
     * pop3+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
     * </pre>
     */
    public static ServerSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = null;
        String password = null;

        URI pop3Uri;
        try {
            pop3Uri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid Pop3Store URI", use);
        }

        String scheme = pop3Uri.getScheme();
        if (scheme.equals("pop3")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = 110;
        } else if (scheme.equals("pop3+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_OPTIONAL;
            port = 110;
        } else if (scheme.equals("pop3+tls+")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = 110;
        } else if (scheme.equals("pop3+ssl+")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = 995;
        } else if (scheme.equals("pop3+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_OPTIONAL;
            port = 995;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = pop3Uri.getHost();

        if (pop3Uri.getPort() != -1) {
            port = pop3Uri.getPort();
        }

        String authType = AuthType.PLAIN.name();
        if (pop3Uri.getUserInfo() != null) {
            try {
                int userIndex = 0, passwordIndex = 1;
                String userinfo = pop3Uri.getUserInfo();
                String[] userInfoParts = userinfo.split(":");
                if (userInfoParts.length > 2 || userinfo.endsWith(":") ) {
                    // If 'userinfo' ends with ":" the password is empty. This can only happen
                    // after an account was imported (so authType and username are present).
                    userIndex++;
                    passwordIndex++;
                    authType = userInfoParts[0];
                }
                username = URLDecoder.decode(userInfoParts[userIndex], "UTF-8");
                if (userInfoParts.length > passwordIndex) {
                    password = URLDecoder.decode(userInfoParts[passwordIndex], "UTF-8");
                }
            } catch (UnsupportedEncodingException enc) {
                // This shouldn't happen since the encoding is hardcoded to UTF-8
                throw new IllegalArgumentException("Couldn't urldecode username or password.", enc);
            }
        }

        return new ServerSettings(STORE_TYPE, host, port, connectionSecurity, authType, username,
                password);
    }

    /**
     * Creates a Pop3Store URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A Pop3Store URI that holds the same information as the {@code server} parameter.
     *
     * @see Account#getStoreUri()
     * @see Pop3Store#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc;
        String passwordEnc;
        try {
            userEnc = URLEncoder.encode(server.username, "UTF-8");
            passwordEnc = (server.password != null) ?
                    URLEncoder.encode(server.password, "UTF-8") : "";
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Could not encode username or password", e);
        }

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_OPTIONAL:
                scheme = "pop3+ssl";
                break;
            case SSL_TLS_REQUIRED:
                scheme = "pop3+ssl+";
                break;
            case STARTTLS_OPTIONAL:
                scheme = "pop3+tls";
                break;
            case STARTTLS_REQUIRED:
                scheme = "pop3+tls+";
                break;
            default:
            case NONE:
                scheme = "pop3";
                break;
        }

        try {
            AuthType.valueOf(server.authenticationType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid authentication type (" +
                    server.authenticationType + ")");
        }

        String userInfo = server.authenticationType + ":" + userEnc + ":" + passwordEnc;
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
    private AuthType mAuthType;
    private int mConnectionSecurity;
    private HashMap<String, Folder> mFolders = new HashMap<String, Folder>();
    private Pop3Capabilities mCapabilities;

    /**
     * This value is {@code true} if the server supports the CAPA command but doesn't advertise
     * support for the TOP command OR if the server doesn't support the CAPA command and we
     * already unsuccessfully tried to use the TOP command.
     */
    private boolean mTopNotSupported;


    public Pop3Store(Account account) throws MessagingException {
        super(account);

        ServerSettings settings;
        try {
            settings = decodeUri(mAccount.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        switch (settings.connectionSecurity) {
        case NONE:
            mConnectionSecurity = CONNECTION_SECURITY_NONE;
            break;
        case STARTTLS_OPTIONAL:
            mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
            break;
        case STARTTLS_REQUIRED:
            mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
            break;
        case SSL_TLS_OPTIONAL:
            mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
            break;
        case SSL_TLS_REQUIRED:
            mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
            break;
        }

        mUsername = settings.username;
        mPassword = settings.password;
        mAuthType = AuthType.valueOf(settings.authenticationType);
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
        folders.add(getFolder(mAccount.getInboxFolderName()));
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mAccount.getInboxFolderName());
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

    class Pop3Folder extends Folder {
        private Socket mSocket;
        private InputStream mIn;
        private OutputStream mOut;
        private HashMap<String, Pop3Message> mUidToMsgMap = new HashMap<String, Pop3Message>();
        private HashMap<Integer, Pop3Message> mMsgNumToMsgMap = new HashMap<Integer, Pop3Message>();
        private HashMap<String, Integer> mUidToMsgNumMap = new HashMap<String, Integer>();
        private String mName;
        private int mMessageCount;

        public Pop3Folder(String name) {
            super(Pop3Store.this.mAccount);
            this.mName = name;

            if (mName.equalsIgnoreCase(mAccount.getInboxFolderName())) {
                mName = mAccount.getInboxFolderName();
            }
        }

        @Override
        public synchronized void open(int mode) throws MessagingException {
            if (isOpen()) {
                return;
            }

            if (!mName.equalsIgnoreCase(mAccount.getInboxFolderName())) {
                throw new MessagingException("Folder does not exist");
            }

            try {
                SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
                if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED ||
                        mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    final boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
                    sslContext.init(null, new TrustManager[] {
                                        TrustManagerFactory.get(mHost, secure)
                                    }, new SecureRandom());
                    mSocket = TrustedSocketFactory.createSocket(sslContext);
                } else {
                    mSocket = new Socket();
                }

                mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
                mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);

                mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                if (!isOpen()) {
                    throw new MessagingException("Unable to connect socket");
                }

                // Eat the banner
                executeSimpleCommand(null);

                if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL
                        || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                    mCapabilities = getCapabilities();

                    if (mCapabilities.stls) {
                        writeLine(STLS_COMMAND);

                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
                        sslContext.init(null, new TrustManager[] {
                                            TrustManagerFactory.get(mHost, secure)
                                        }, new SecureRandom());
                        mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort,
                                  true);
                        mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
                        mIn = new BufferedInputStream(mSocket.getInputStream(), 1024);
                        mOut = new BufferedOutputStream(mSocket.getOutputStream(), 512);
                        if (!isOpen()) {
                            throw new MessagingException("Unable to connect socket");
                        }
                    } else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
                        throw new MessagingException("TLS not supported but required");
                    }
                }

                if (mAuthType == AuthType.CRAM_MD5) {
                    try {
                        String b64Nonce = executeSimpleCommand("AUTH CRAM-MD5").replace("+ ", "");

                        String b64CRAM = Authentication.computeCramMd5(mUsername, mPassword, b64Nonce);
                        executeSimpleCommand(b64CRAM);

                    } catch (MessagingException me) {
                        throw new AuthenticationFailedException(null, me);
                    }
                } else {
                    try {
                        executeSimpleCommand(USER_COMMAND + " " + mUsername);
                        executeSimpleCommand(PASS_COMMAND + " " + mPassword, true);
                    } catch (MessagingException me) {
                        throw new AuthenticationFailedException(null, me);
                    }
                }

                mCapabilities = getCapabilities();
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
            return mName.equalsIgnoreCase(mAccount.getInboxFolderName());
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
        public Message getMessage(String uid) throws MessagingException {
            Pop3Message message = mUidToMsgMap.get(uid);
            if (message == null) {
                message = new Pop3Message(uid, this);
            }
            return message;
        }

        @Override
        public Message[] getMessages(int start, int end, Date earliestDate, MessageRetrievalListener listener)
        throws MessagingException {
            if (start < 1 || end < 1 || end < start) {
                throw new MessagingException(String.format("Invalid message set %d %d",
                                             start, end));
            }
            try {
                indexMsgNums(start, end);
            } catch (IOException ioe) {
                throw new MessagingException("getMessages", ioe);
            }
            ArrayList<Message> messages = new ArrayList<Message>();
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
            return messages.toArray(new Message[messages.size()]);
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
                            Log.e(K9.LOG_TAG, "ERR response: " + response);
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

        private void indexUids(ArrayList<String> uids)
        throws MessagingException, IOException {
            HashSet<String> unindexedUids = new HashSet<String>();
            for (String uid : uids) {
                if (mUidToMsgMap.get(uid) == null) {
                    if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                        Log.d(K9.LOG_TAG, "Need to index UID " + uid);
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
                        if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                            Log.d(K9.LOG_TAG, "Got msgNum " + msgNum + " for UID " + msgUid);
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
            if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                Log.d(K9.LOG_TAG, "Adding index for UID " + message.getUid() + " to msgNum " + msgNum);
            }
            mMsgNumToMsgMap.put(msgNum, message);
            mUidToMsgMap.put(message.getUid(), message);
            mUidToMsgNumMap.put(message.getUid(), msgNum);
        }

        @Override
        public Message[] getMessages(MessageRetrievalListener listener) throws MessagingException {
            throw new UnsupportedOperationException("Pop3: No getMessages");
        }

        @Override
        public Message[] getMessages(String[] uids, MessageRetrievalListener listener)
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
        public void fetch(Message[] messages, FetchProfile fp, MessageRetrievalListener listener)
        throws MessagingException {
            if (messages == null || messages.length == 0) {
                return;
            }
            ArrayList<String> uids = new ArrayList<String>();
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
            for (int i = 0, count = messages.length; i < count; i++) {
                Message message = messages[i];
                if (!(message instanceof Pop3Message)) {
                    throw new MessagingException("Pop3Store.fetch called with non-Pop3 Message");
                }
                Pop3Message pop3Message = (Pop3Message)message;
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
                        if (mAccount.getMaximumAutoDownloadMessageSize() > 0) {
                            fetchBody(pop3Message,
                                      (mAccount.getMaximumAutoDownloadMessageSize() / 76));
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
                        listener.messageFinished(message, i, count);
                    }
                } catch (IOException ioe) {
                    throw new MessagingException("Unable to fetch message", ioe);
                }
            }
        }

        private void fetchEnvelope(Message[] messages,
                                   MessageRetrievalListener listener)  throws IOException, MessagingException {
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
                for (int i = 0, count = messages.length; i < count; i++) {
                    Message message = messages[i];
                    if (!(message instanceof Pop3Message)) {
                        throw new MessagingException("Pop3Store.fetch called with non-Pop3 Message");
                    }
                    Pop3Message pop3Message = (Pop3Message)message;
                    if (listener != null) {
                        listener.messageStarted(pop3Message.getUid(), i, count);
                    }
                    String response = executeSimpleCommand(String.format(LIST_COMMAND + " %d",
                                                           mUidToMsgNumMap.get(pop3Message.getUid())));
                    String[] listParts = response.split(" ");
                    //int msgNum = Integer.parseInt(listParts[1]);
                    int msgSize = Integer.parseInt(listParts[2]);
                    pop3Message.setSize(msgSize);
                    if (listener != null) {
                        listener.messageFinished(pop3Message, i, count);
                    }
                }
            } else {
                HashSet<String> msgUidIndex = new HashSet<String>();
                for (Message message : messages) {
                    msgUidIndex.add(message.getUid());
                }
                int i = 0, count = messages.length;
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
                    if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3 && !mCapabilities.top) {
                        Log.d(K9.LOG_TAG, "This server doesn't support the CAPA command. " +
                              "Checking to see if the TOP command is supported nevertheless.");
                    }

                    response = executeSimpleCommand(String.format(TOP_COMMAND + " %d %d",
                                                    mUidToMsgNumMap.get(message.getUid()), lines));

                    // TOP command is supported. Remember this for the next time.
                    mCapabilities.top = true;
                } catch (Pop3ErrorResponse e) {
                    if (mCapabilities.top) {
                        // The TOP command should be supported but something went wrong.
                        throw e;
                    } else {
                        if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                            Log.d(K9.LOG_TAG, "The server really doesn't support the TOP " +
                                  "command. Using RETR instead.");
                        }

                        // Don't try to use the TOP command again.
                        mTopNotSupported = true;
                    }
                }
            }

            if (response == null) {
                executeSimpleCommand(String.format(RETR_COMMAND + " %d",
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
        public Map<String, String> appendMessages(Message[] messages) throws MessagingException {
            return null;
        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
        }

        @Override
        public void delete(Message[] msgs, String trashFolderName) throws MessagingException {
            setFlags(msgs, new Flag[] { Flag.DELETED }, true);
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            return null;
        }

        @Override
        public void setFlags(Flag[] flags, boolean value) throws MessagingException {
            throw new UnsupportedOperationException("POP3: No setFlags(Flag[],boolean)");
        }

        @Override
        public void setFlags(Message[] messages, Flag[] flags, boolean value)
        throws MessagingException {
            if (!value || !Utility.arrayContains(flags, Flag.DELETED)) {
                /*
                 * The only flagging we support is setting the Deleted flag.
                 */
                return;
            }
            ArrayList<String> uids = new ArrayList<String>();
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
            if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                Log.d(K9.LOG_TAG, "<<< " + ret);
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
                String response = executeSimpleCommand(CAPA_COMMAND);
                while ((response = readLine()) != null) {
                    if (response.equals(".")) {
                        break;
                    }
                    if (response.equalsIgnoreCase(STLS_CAPABILITY)) {
                        capabilities.stls = true;
                    } else if (response.equalsIgnoreCase(UIDL_CAPABILITY)) {
                        capabilities.uidl = true;
                    } else if (response.equalsIgnoreCase(PIPELINING_CAPABILITY)) {
                        capabilities.pipelining = true;
                    } else if (response.equalsIgnoreCase(USER_CAPABILITY)) {
                        capabilities.user = true;
                    } else if (response.equalsIgnoreCase(TOP_CAPABILITY)) {
                        capabilities.top = true;
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
                    if (K9.DEBUG && K9.DEBUG_PROTOCOL_POP3) {
                        if (sensitive && !K9.DEBUG_SENSITIVE) {
                            Log.d(K9.LOG_TAG, ">>> "
                                  + "[Command Hidden, Enable Sensitive Debug Logging To Show]");
                        } else {
                            Log.d(K9.LOG_TAG, ">>> " + command);
                        }
                    }

                    writeLine(command);
                }

                String response = readLine();
                if (response.length() > 1 && response.charAt(0) == '-') {
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
        protected void parse(InputStream in) throws IOException, MessagingException {
            super.parse(in);
        }

        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(new Message[] { this }, new Flag[] { flag }, set);
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
//          Log.w(K9.LOG_TAG, "Could not delete non-existent message", me);
//         }
        }
    }

    static class Pop3Capabilities {
        public boolean stls;
        public boolean top;
        public boolean user;
        public boolean uidl;
        public boolean pipelining;

        @Override
        public String toString() {
            return String.format("STLS %b, TOP %b, USER %b, UIDL %b, PIPELINING %b",
                                 stls,
                                 top,
                                 user,
                                 uidl,
                                 pipelining);
        }
    }

    static class Pop3ResponseInputStream extends InputStream {
        InputStream mIn;
        boolean mStartOfLine = true;
        boolean mFinished;

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
