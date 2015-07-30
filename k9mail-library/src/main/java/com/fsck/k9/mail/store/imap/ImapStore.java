
package com.fsck.k9.mail.store.imap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.internet.MimeMessageHelper;
import com.fsck.k9.mail.power.TracingPowerManager;
import com.fsck.k9.mail.power.TracingPowerManager.TracingWakeLock;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.filter.EOLConvertingOutputStream;
import com.fsck.k9.mail.filter.FixedLengthInputStream;
import com.fsck.k9.mail.internet.MimeBodyPart;
import com.fsck.k9.mail.internet.MimeHeader;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.internet.MimeMultipart;
import com.fsck.k9.mail.internet.MimeUtility;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;

import com.beetstra.jutf7.CharsetProvider;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;
import static com.fsck.k9.mail.K9MailLib.PUSH_WAKE_LOCK_TIMEOUT;

/**
 * <pre>
 * TODO Need to start keeping track of UIDVALIDITY
 * TODO Need a default response handler for things like folder updates
 * </pre>
 */
public class ImapStore extends RemoteStore {

    private static final int IDLE_READ_TIMEOUT_INCREMENT = 5 * 60 * 1000;
    private static final int IDLE_FAILURE_COUNT_LIMIT = 10;
    private static final int MAX_DELAY_TIME = 5 * 60 * 1000; // 5 minutes
    private static final int NORMAL_DELAY_TIME = 5000;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final int FETCH_WINDOW_SIZE = 100;
    private Set<Flag> mPermanentFlagsIndex = EnumSet.noneOf(Flag.class);
    private ConnectivityManager mConnectivityManager;

    private String mHost;
    private int mPort;
    private String mUsername;
    private String mPassword;
    private String mClientCertificateAlias;
    private ConnectionSecurity mConnectionSecurity;
    private AuthType mAuthType;
    private String mPathPrefix;
    private String mCombinedPrefix = null;
    private String mPathDelimiter = null;

    /**
     * Decodes an ImapStore URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * imap://auth:user:password@server:port ConnectionSecurity.NONE
     * imap+tls+://auth:user:password@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * imap+ssl+://auth:user:password@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     *
     * @param uri the store uri. NOTE: this method expects the userinfo part of the uri to be
     * encoded twice, due to a bug in {@link #createUri(ServerSettings)}.
     */
    public static ImapStoreSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authenticationType = null;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;
        String pathPrefix = null;
        boolean autoDetectNamespace = true;

        URI imapUri;
        try {
            imapUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid ImapStore URI", use);
        }

        String scheme = imapUri.getScheme();
        /*
         * Currently available schemes are:
         * imap
         * imap+tls+
         * imap+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * imap+tls
         * imap+ssl
         */
        if (scheme.equals("imap")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = Type.IMAP.defaultPort;
        } else if (scheme.startsWith("imap+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = Type.IMAP.defaultPort;
        } else if (scheme.startsWith("imap+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = Type.IMAP.defaultTlsPort;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = imapUri.getHost();

        if (imapUri.getPort() != -1) {
            port = imapUri.getPort();
        }

        if (imapUri.getUserInfo() != null) {
            String userinfo = imapUri.getUserInfo();
            String[] userInfoParts = userinfo.split(":");

            if (userinfo.endsWith(":")) {
                // Password is empty. This can only happen after an account was imported.
                authenticationType = AuthType.valueOf(userInfoParts[0]);
                username = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 2) {
                authenticationType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 3) {
                authenticationType = AuthType.valueOf(userInfoParts[0]);
                username = decodeUtf8(userInfoParts[1]);

                if (AuthType.EXTERNAL == authenticationType) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[2]);
                } else {
                    password = decodeUtf8(userInfoParts[2]);
                }
            }
        }

        String path = imapUri.getPath();
        if (path != null && path.length() > 1) {
            // Strip off the leading "/"
            String cleanPath = path.substring(1);

            if (cleanPath.length() >= 2 && cleanPath.charAt(1) == '|') {
                autoDetectNamespace = cleanPath.charAt(0) == '1';
                if (!autoDetectNamespace) {
                    pathPrefix = cleanPath.substring(2);
                }
            } else {
                if (cleanPath.length() > 0) {
                    pathPrefix = cleanPath;
                    autoDetectNamespace = false;
                }
            }
        }

        return new ImapStoreSettings(host, port, connectionSecurity, authenticationType, username,
                password, clientCertificateAlias, autoDetectNamespace, pathPrefix);
    }

    /**
     * Creates an ImapStore URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return An ImapStore URI that holds the same information as the {@code server} parameter.
     *
     * @see com.fsck.k9.mail.store.StoreConfig#getStoreUri()
     * @see ImapStore#decodeUri(String)
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
                scheme = "imap+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "imap+tls+";
                break;
            default:
            case NONE:
                scheme = "imap";
                break;
        }

        AuthType authType = server.authenticationType;
        String userInfo;
        if (authType == AuthType.EXTERNAL) {
            userInfo = authType.name() + ":" + userEnc + ":" + clientCertificateAliasEnc;
        } else {
            userInfo = authType.name() + ":" + userEnc + ":" + passwordEnc;
        }
        try {
            Map<String, String> extra = server.getExtra();
            String path;
            if (extra != null) {
                boolean autoDetectNamespace = Boolean.TRUE.toString().equals(
                        extra.get(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY));
                String pathPrefix = (autoDetectNamespace) ?
                        null : extra.get(ImapStoreSettings.PATH_PREFIX_KEY);
                path = "/" + (autoDetectNamespace ? "1" : "0") + "|" +
                    ((pathPrefix == null) ? "" : pathPrefix);
            } else {
                path = "/1|";
            }
            return new URI(scheme, userInfo, server.host, server.port,
                path,
                null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create ImapStore URI", e);
        }
    }

    /**
     * This class is used to store the decoded contents of an ImapStore URI.
     *
     * @see ImapStore#decodeUri(String)
     */
    public static class ImapStoreSettings extends ServerSettings {
        public static final String AUTODETECT_NAMESPACE_KEY = "autoDetectNamespace";
        public static final String PATH_PREFIX_KEY = "pathPrefix";

        public final boolean autoDetectNamespace;
        public final String pathPrefix;

        protected ImapStoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
                AuthType authenticationType, String username, String password, String clientCertificateAlias,
                boolean autodetectNamespace, String pathPrefix) {
            super(Type.IMAP, host, port, connectionSecurity, authenticationType, username,
                    password, clientCertificateAlias);
            this.autoDetectNamespace = autodetectNamespace;
            this.pathPrefix = pathPrefix;
        }

        @Override
        public Map<String, String> getExtra() {
            Map<String, String> extra = new HashMap<String, String>();
            extra.put(AUTODETECT_NAMESPACE_KEY, Boolean.valueOf(autoDetectNamespace).toString());
            putIfNotNull(extra, PATH_PREFIX_KEY, pathPrefix);
            return extra;
        }

        @Override
        public ServerSettings newPassword(String newPassword) {
            return new ImapStoreSettings(host, port, connectionSecurity, authenticationType,
                    username, newPassword, clientCertificateAlias, autoDetectNamespace, pathPrefix);
        }
    }


    protected static final SimpleDateFormat RFC3501_DATE = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);

    private final Deque<ImapConnection> mConnections =
        new LinkedList<ImapConnection>();

    /**
     * Charset used for converting folder names to and from UTF-7 as defined by RFC 3501.
     */
    private Charset mModifiedUtf7Charset;

    /**
     * Cache of ImapFolder objects. ImapFolders are attached to a given folder on the server
     * and as long as their associated connection remains open they are reusable between
     * requests. This cache lets us make sure we always reuse, if possible, for a given
     * folder name.
     */
    private final Map<String, ImapFolder> mFolderCache = new HashMap<String, ImapFolder>();

    public ImapStore(StoreConfig storeConfig,
                     TrustedSocketFactory trustedSocketFactory,
                     ConnectivityManager connectivityManager)
            throws MessagingException {
        super(storeConfig, trustedSocketFactory);

        ImapStoreSettings settings;
        try {
            settings = decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        mHost = settings.host;
        mPort = settings.port;

        mConnectionSecurity = settings.connectionSecurity;
        mConnectivityManager = connectivityManager;

        mAuthType = settings.authenticationType;
        mUsername = settings.username;
        mPassword = settings.password;
        mClientCertificateAlias = settings.clientCertificateAlias;

        // Make extra sure mPathPrefix is null if "auto-detect namespace" is configured
        mPathPrefix = (settings.autoDetectNamespace) ? null : settings.pathPrefix;

        mModifiedUtf7Charset = new CharsetProvider().charsetForName("X-RFC-3501");
    }

    @Override
    public Folder getFolder(String name) {
        ImapFolder folder;
        synchronized (mFolderCache) {
            folder = mFolderCache.get(name);
            if (folder == null) {
                folder = new ImapFolder(this, name);
                mFolderCache.put(name, folder);
            }
        }
        return folder;
    }

    private String getCombinedPrefix() {
        if (mCombinedPrefix == null) {
            if (mPathPrefix != null) {
                String tmpPrefix = mPathPrefix.trim();
                String tmpDelim = (mPathDelimiter != null ? mPathDelimiter.trim() : "");
                if (tmpPrefix.endsWith(tmpDelim)) {
                    mCombinedPrefix = tmpPrefix;
                } else if (tmpPrefix.length() > 0) {
                    mCombinedPrefix = tmpPrefix + tmpDelim;
                } else {
                    mCombinedPrefix = "";
                }
            } else {
                mCombinedPrefix = "";
            }
        }
        return mCombinedPrefix;
    }

    @Override
    public List <? extends Folder > getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        ImapConnection connection = getConnection();
        try {
            List <? extends Folder > allFolders = listFolders(connection, false);
            if (forceListAll || !mStoreConfig.subscribedFoldersOnly()) {
                return allFolders;
            } else {
                List<Folder> resultFolders = new LinkedList<Folder>();
                Set<String> subscribedFolderNames = new HashSet<String>();
                List <? extends Folder > subscribedFolders = listFolders(connection, true);
                for (Folder subscribedFolder : subscribedFolders) {
                    subscribedFolderNames.add(subscribedFolder.getName());
                }
                for (Folder folder : allFolders) {
                    if (subscribedFolderNames.contains(folder.getName())) {
                        resultFolders.add(folder);
                    }
                }
                return resultFolders;
            }
        } catch (IOException ioe) {
            connection.close();
            throw new MessagingException("Unable to get folder list.", ioe);
        } catch (MessagingException me) {
            connection.close();
            throw new MessagingException("Unable to get folder list.", me);
        } finally {
            releaseConnection(connection);
        }
    }


    private List <? extends Folder > listFolders(ImapConnection connection, boolean LSUB) throws IOException, MessagingException {
        String commandResponse = LSUB ? "LSUB" : "LIST";

        LinkedList<Folder> folders = new LinkedList<Folder>();

        List<ImapResponse> responses =
            connection.executeSimpleCommand(String.format("%s \"\" %s", commandResponse,
                                            encodeString(getCombinedPrefix() + "*")));

        for (ImapResponse response : responses) {
            if (ImapResponseParser.equalsIgnoreCase(response.get(0), commandResponse)) {
                boolean includeFolder = true;

                if (response.size() > 4 || !(response.getObject(3) instanceof String)) {
                    Log.w(LOG_TAG, "Skipping incorrectly parsed " + commandResponse +
                            " reply: " + response);
                    continue;
                }

                String decodedFolderName;
                try {
                    decodedFolderName = decodeFolderName(response.getString(3));
                } catch (CharacterCodingException e) {
                    Log.w(LOG_TAG, "Folder name not correctly encoded with the UTF-7 variant " +
                          "as defined by RFC 3501: " + response.getString(3), e);

                    //TODO: Use the raw name returned by the server for all commands that require
                    //      a folder name. Use the decoded name only for showing it to the user.

                    // We currently just skip folders with malformed names.
                    continue;
                }

                String folder = decodedFolderName;

                if (mPathDelimiter == null) {
                    mPathDelimiter = response.getString(2);
                    mCombinedPrefix = null;
                }

                if (folder.equalsIgnoreCase(mStoreConfig.getInboxFolderName())) {
                    continue;
                } else if (folder.equals(mStoreConfig.getOutboxFolderName())) {
                    /*
                     * There is a folder on the server with the same name as our local
                     * outbox. Until we have a good plan to deal with this situation
                     * we simply ignore the folder on the server.
                     */
                    continue;
                } else {
                    int prefixLength = getCombinedPrefix().length();
                    if (prefixLength > 0) {
                        // Strip prefix from the folder name
                        if (folder.length() >= prefixLength) {
                            folder = folder.substring(prefixLength);
                        }
                        if (!decodedFolderName.equalsIgnoreCase(getCombinedPrefix() + folder)) {
                            includeFolder = false;
                        }
                    }
                }

                ImapList attributes = response.getList(1);
                for (int i = 0, count = attributes.size(); i < count; i++) {
                    String attribute = attributes.getString(i);
                    if (attribute.equalsIgnoreCase("\\NoSelect")) {
                        includeFolder = false;
                    }
                }
                if (includeFolder) {
                    folders.add(getFolder(folder));
                }
            }
        }
        folders.add(getFolder(mStoreConfig.getInboxFolderName()));
        return folders;

    }

    /**
     * Attempt to auto-configure folders by attributes if the server advertises that capability.
     *
     * The parsing here is essentially the same as
     * {@link #listFolders(ImapConnection, boolean)}; we should try to consolidate
     * this at some point. :(
     * @param connection IMAP Connection
     * @throws IOException uh oh!
     * @throws MessagingException uh oh!
     */
    private void autoconfigureFolders(final ImapConnection connection) throws IOException, MessagingException {
        String commandResponse;
        String commandOptions = "";

        if (connection.getCapabilities().contains("XLIST")) {
            if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration: Using XLIST.");
            commandResponse = "XLIST";
        } else if(connection.getCapabilities().contains("SPECIAL-USE")) {
            if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration: Using RFC6154/SPECIAL-USE.");
            commandResponse = "LIST";
            commandOptions = " (SPECIAL-USE)";
        } else {
            if (K9MailLib.isDebug()) Log.d(LOG_TAG, "No detected folder auto-configuration methods.");
            return;
        }

        final List<ImapResponse> responses =
            connection.executeSimpleCommand(String.format("%s%s \"\" %s", commandResponse, commandOptions,
                encodeString(getCombinedPrefix() + "*")));

        for (ImapResponse response : responses) {
            if (ImapResponseParser.equalsIgnoreCase(response.get(0), commandResponse)) {

                String decodedFolderName;
                try {
                    decodedFolderName = decodeFolderName(response.getString(3));
                } catch (CharacterCodingException e) {
                    Log.w(LOG_TAG, "Folder name not correctly encoded with the UTF-7 variant " +
                        "as defined by RFC 3501: " + response.getString(3), e);
                    // We currently just skip folders with malformed names.
                    continue;
                }

                if (mPathDelimiter == null) {
                    mPathDelimiter = response.getString(2);
                    mCombinedPrefix = null;
                }

                ImapList attributes = response.getList(1);
                for (int i = 0, count = attributes.size(); i < count; i++) {
                    String attribute = attributes.getString(i);
                    if (attribute.equals("\\Drafts")) {
                        mStoreConfig.setDraftsFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration detected draft folder: " + decodedFolderName);
                    } else if (attribute.equals("\\Sent")) {
                        mStoreConfig.setSentFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration detected sent folder: " + decodedFolderName);
                    } else if (attribute.equals("\\Spam") || attribute.equals("\\Junk")) {
                        //rfc6154 just mentions \Junk
                        mStoreConfig.setSpamFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration detected spam folder: " + decodedFolderName);
                    } else if (attribute.equals("\\Trash")) {
                        mStoreConfig.setTrashFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration detected trash folder: " + decodedFolderName);
                    }
                }
            }
        }
    }

    @Override
    public void checkSettings() throws MessagingException {
        try {
            ImapConnection connection = new ImapConnection(
                    new StoreImapSettings(),
                    mTrustedSocketFactory,
                    mConnectivityManager);

            connection.open();
            autoconfigureFolders(connection);
            connection.close();
        } catch (IOException ioe) {
            throw new MessagingException("Unable to connect", ioe);
        }
    }

    private ImapConnection getConnection() throws MessagingException {
        synchronized (mConnections) {
            ImapConnection connection;
            while ((connection = mConnections.poll()) != null) {
                try {
                    connection.executeSimpleCommand("NOOP");
                    break;
                } catch (IOException ioe) {
                    connection.close();
                }
            }
            if (connection == null) {
                connection = new ImapConnection(new StoreImapSettings(),
                        mTrustedSocketFactory,
                        mConnectivityManager);
            }
            return connection;
        }
    }

    private void releaseConnection(ImapConnection connection) {
        if (connection != null && connection.isOpen()) {
            synchronized (mConnections) {
                mConnections.offer(connection);
            }
        }
    }

    /**
     * Encode a string to be able to use it in an IMAP command.
     *
     * "A quoted string is a sequence of zero or more 7-bit characters,
     *  excluding CR and LF, with double quote (<">) characters at each
     *  end." - Section 4.3, RFC 3501
     *
     * Double quotes and backslash are escaped by prepending a backslash.
     *
     * @param str
     *     The input string (only 7-bit characters allowed).
     * @return
     *     The string encoded as quoted (IMAP) string.
     */
    private static String encodeString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String encodeFolderName(String name) {
        ByteBuffer bb = mModifiedUtf7Charset.encode(name);
        byte[] b = new byte[bb.limit()];
        bb.get(b);
        return new String(b, Charset.forName("US-ASCII"));
    }

    private String decodeFolderName(String name) throws CharacterCodingException {
        /*
         * Convert the encoded name to US-ASCII, then pass it through the modified UTF-7
         * decoder and return the Unicode String.
         */
        // Make sure the decoder throws an exception if it encounters an invalid encoding.
        CharsetDecoder decoder = mModifiedUtf7Charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
        CharBuffer cb = decoder.decode(ByteBuffer.wrap(name.getBytes(Charset.forName("US-ASCII"))));
        return cb.toString();

    }

    @Override
    public boolean isMoveCapable() {
        return true;
    }

    @Override
    public boolean isCopyCapable() {
        return true;
    }
    @Override
    public boolean isPushCapable() {
        return true;
    }
    @Override
    public boolean isExpungeCapable() {
        return true;
    }


    protected class ImapFolder extends Folder<ImapMessage> {
        private String mName;
        protected volatile int mMessageCount = -1;
        protected volatile long uidNext = -1L;
        protected volatile ImapConnection mConnection;
        private int mMode;
        private volatile boolean mExists;
        private ImapStore store = null;
        Map<Long, String> msgSeqUidMap = new ConcurrentHashMap<Long, String>();
        private boolean mInSearch = false;

        public ImapFolder(ImapStore nStore, String name) {
            super();
            store = nStore;
            this.mName = name;
        }

        public String getPrefixedName() throws MessagingException {
            String prefixedName = "";
            if (!mStoreConfig.getInboxFolderName().equalsIgnoreCase(mName)) {
                ImapConnection connection;
                synchronized (this) {
                    if (mConnection == null) {
                        connection = getConnection();
                    } else {
                        connection = mConnection;
                    }
                }
                try {

                    connection.open();
                } catch (IOException ioe) {
                    throw new MessagingException("Unable to get IMAP prefix", ioe);
                } finally {
                    if (mConnection == null) {
                        releaseConnection(connection);
                    }
                }
                prefixedName = getCombinedPrefix();
            }

            prefixedName += mName;
            return prefixedName;
        }

        protected List<ImapResponse> executeSimpleCommand(String command) throws MessagingException, IOException {
            return handleUntaggedResponses(mConnection.executeSimpleCommand(command));
        }

        protected List<ImapResponse> executeSimpleCommand(String command, boolean sensitve, UntaggedHandler untaggedHandler) throws MessagingException, IOException {
            return handleUntaggedResponses(mConnection.executeSimpleCommand(command, sensitve, untaggedHandler));
        }

        @Override
        public void open(int mode) throws MessagingException {
            internalOpen(mode);

            if (mMessageCount == -1) {
                throw new MessagingException(
                    "Did not find message count during open");
            }
        }

        public List<ImapResponse> internalOpen(int mode) throws MessagingException {
            if (isOpen() && mMode == mode) {
                // Make sure the connection is valid. If it's not we'll close it down and continue
                // on to get a new one.
                try {
                    return executeSimpleCommand("NOOP");
                } catch (IOException ioe) {
                    /* don't throw */ ioExceptionHandler(mConnection, ioe);
                }
            }
            releaseConnection(mConnection);
            synchronized (this) {
                mConnection = getConnection();
            }
            // * FLAGS (\Answered \Flagged \Deleted \Seen \Draft NonJunk
            // $MDNSent)
            // * OK [PERMANENTFLAGS (\Answered \Flagged \Deleted \Seen \Draft
            // NonJunk $MDNSent \*)] Flags permitted.
            // * 23 EXISTS
            // * 0 RECENT
            // * OK [UIDVALIDITY 1125022061] UIDs valid
            // * OK [UIDNEXT 57576] Predicted next UID
            // 2 OK [READ-WRITE] Select completed.
            try {
                msgSeqUidMap.clear();
                String command = String.format("%s %s", mode == OPEN_MODE_RW ? "SELECT"
                        : "EXAMINE", encodeString(encodeFolderName(getPrefixedName())));

                List<ImapResponse> responses = executeSimpleCommand(command);

                /*
                 * If the command succeeds we expect the folder has been opened read-write unless we
                 * are notified otherwise in the responses.
                 */
                mMode = mode;

                for (ImapResponse response : responses) {
                    if (response.size() >= 2) {
                        Object bracketedObj = response.get(1);
                        if (!(bracketedObj instanceof ImapList)) {
                            continue;
                        }
                        ImapList bracketed = (ImapList) bracketedObj;
                        if (bracketed.isEmpty()) {
                            continue;
                        }

                        ImapList flags = bracketed.getKeyedList("PERMANENTFLAGS");
                        if (flags != null) {
                            // parse: * OK [PERMANENTFLAGS (\Answered \Flagged \Deleted
                            // \Seen \Draft NonJunk $label1 \*)] Flags permitted.
                            parseFlags(flags);
                        } else {
                            Object keyObj = bracketed.get(0);
                            if (keyObj instanceof String) {
                                String key = (String) keyObj;
                                if (response.getTag() != null) {

                                    if ("READ-ONLY".equalsIgnoreCase(key)) {
                                        mMode = OPEN_MODE_RO;
                                    } else if ("READ-WRITE".equalsIgnoreCase(key)) {
                                        mMode = OPEN_MODE_RW;
                                    }
                                }
                            }
                        }
                    }
                }
                mExists = true;
                return responses;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            } catch (MessagingException me) {
                Log.e(LOG_TAG, "Unable to open connection for " + getLogId(), me);
                throw me;
            }
        }

        /**
         * Parses an string like PERMANENTFLAGS (\Answered \Flagged \Deleted // \Seen \Draft NonJunk
         * $label1 \*)
         *
         * the parsed flags are stored in the mPermanentFlagsIndex
         * @param flags
         *            the imapflags as strings
         */
        private void parseFlags(ImapList flags) {
            for (Object flag : flags) {
                flag = flag.toString().toLowerCase(Locale.US);
                if (flag.equals("\\deleted")) {
                    mPermanentFlagsIndex.add(Flag.DELETED);
                } else if (flag.equals("\\answered")) {
                    mPermanentFlagsIndex.add(Flag.ANSWERED);
                } else if (flag.equals("\\seen")) {
                    mPermanentFlagsIndex.add(Flag.SEEN);
                } else if (flag.equals("\\flagged")) {
                    mPermanentFlagsIndex.add(Flag.FLAGGED);
                } else if (flag.equals("$forwarded")) {
                    mPermanentFlagsIndex.add(Flag.FORWARDED);
                } else if (flag.equals("\\*")) {
                    mCanCreateKeywords = true;
                }
            }
        }

        @Override
        public boolean isOpen() {
            return mConnection != null;
        }

        @Override
        public int getMode() {
            return mMode;
        }

        @Override
        public void close() {
            if (mMessageCount != -1) {
                mMessageCount = -1;
            }
            if (!isOpen()) {
                return;
            }

            synchronized (this) {
                // If we are mid-search and we get a close request, we gotta trash the connection.
                if (mInSearch && mConnection != null) {
                    Log.i(LOG_TAG, "IMAP search was aborted, shutting down connection.");
                    mConnection.close();
                } else {
                    releaseConnection(mConnection);
                }
                mConnection = null;
            }
        }

        @Override
        public String getName() {
            return mName;
        }

        /**
         * Check if a given folder exists on the server.
         *
         * @param folderName
         *     The name of the folder encoded as quoted string.
         *     See {@link ImapStore#encodeString}
         *
         * @return
         *     {@code True}, if the folder exists. {@code False}, otherwise.
         */
        private boolean exists(String folderName) throws MessagingException {
            try {
                // Since we don't care about RECENT, we'll use that for the check, because we're checking
                // a folder other than ourself, and don't want any untagged responses to cause a change
                // in our own fields
                mConnection.executeSimpleCommand(String.format("STATUS %s (RECENT)", folderName));
                return true;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            } catch (ImapException ie) {
                // We got a response, but it was not "OK"
                return false;
            }
        }

        @Override
        public boolean exists() throws MessagingException {
            if (mExists) {
                return true;
            }
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection;
            synchronized (this) {
                if (mConnection == null) {
                    connection = getConnection();
                } else {
                    connection = mConnection;
                }
            }
            try {
                connection.executeSimpleCommand(String.format("STATUS %s (UIDVALIDITY)",
                                                encodeString(encodeFolderName(getPrefixedName()))));
                mExists = true;
                return true;
            } catch (ImapException ie) {
                // We got a response, but it was not "OK"
                return false;
            } catch (IOException ioe) {
                throw ioExceptionHandler(connection, ioe);
            } finally {
                if (mConnection == null) {
                    releaseConnection(connection);
                }
            }
        }

        @Override
        public boolean create(FolderType type) throws MessagingException {
            /*
             * This method needs to operate in the unselected mode as well as the selected mode
             * so we must get the connection ourselves if it's not there. We are specifically
             * not calling checkOpen() since we don't care if the folder is open.
             */
            ImapConnection connection;
            synchronized (this) {
                if (mConnection == null) {
                    connection = getConnection();
                } else {
                    connection = mConnection;
                }
            }
            try {
                connection.executeSimpleCommand(String.format("CREATE %s",
                                                encodeString(encodeFolderName(getPrefixedName()))));
                return true;
            } catch (ImapException ie) {
                // We got a response, but it was not "OK"
                return false;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            } finally {
                if (mConnection == null) {
                    releaseConnection(connection);
                }
            }
        }

        /**
         * Copies the given messages to the specified folder.
         *
         * <p>
         * <strong>Note:</strong>
         * Only the UIDs of the given {@link Message} instances are used. It is assumed that all
         * UIDs represent valid messages in this folder.
         * </p>
         *
         * @param messages
         *         The messages to copy to the specfied folder.
         * @param folder
         *         The name of the target folder.
         *
         * @return The mapping of original message UIDs to the new server UIDs.
         */
        @Override
        public Map<String, String> copyMessages(List<? extends Message> messages, Folder folder)
                throws MessagingException {
            if (!(folder instanceof ImapFolder)) {
                throw new MessagingException("ImapFolder.copyMessages passed non-ImapFolder");
            }

            if (messages.isEmpty()) {
                return null;
            }

            ImapFolder iFolder = (ImapFolder)folder;
            checkOpen(); //only need READ access

            String[] uids = new String[messages.size()];
            for (int i = 0, count = messages.size(); i < count; i++) {
                uids[i] = messages.get(i).getUid();
            }

            try {
                String remoteDestName = encodeString(encodeFolderName(iFolder.getPrefixedName()));

                //TODO: Try to copy/move the messages first and only create the folder if the
                //      operation fails. This will save a roundtrip if the folder already exists.
                if (!exists(remoteDestName)) {
                    /*
                     * If the remote folder doesn't exist we try to create it.
                     */
                    if (K9MailLib.isDebug()) {
                        Log.i(LOG_TAG, "ImapFolder.copyMessages: attempting to create remote " +
                                "folder '" + remoteDestName + "' for " + getLogId());
                    }

                    iFolder.create(FolderType.HOLDS_MESSAGES);
                }

                //TODO: Split this into multiple commands if the command exceeds a certain length.
                List<ImapResponse> responses = executeSimpleCommand(String.format("UID COPY %s %s",
                                                      combine(uids, ','),
                                                      remoteDestName));

                // Get the tagged response for the UID COPY command
                ImapResponse response = responses.get(responses.size() - 1);

                Map<String, String> uidMap = null;
                if (response.size() > 1) {
                    /*
                     * If the server supports UIDPLUS, then along with the COPY response it will
                     * return an COPYUID response code, e.g.
                     *
                     * 24 OK [COPYUID 38505 304,319:320 3956:3958] Success
                     *
                     * COPYUID is followed by UIDVALIDITY, the set of UIDs of copied messages from
                     * the source folder and the set of corresponding UIDs assigned to them in the
                     * destination folder.
                     *
                     * We can use the new UIDs included in this response to update our records.
                     */
                    Object responseList = response.get(1);

                    if (responseList instanceof ImapList) {
                        final ImapList copyList = (ImapList) responseList;
                        if (copyList.size() >= 4 && copyList.getString(0).equals("COPYUID")) {
                            List<String> srcUids = ImapUtility.getImapSequenceValues(
                                    copyList.getString(2));
                            List<String> destUids = ImapUtility.getImapSequenceValues(
                                    copyList.getString(3));

                            if (srcUids != null && destUids != null) {
                                if (srcUids.size() == destUids.size()) {
                                    Iterator<String> srcUidsIterator = srcUids.iterator();
                                    Iterator<String> destUidsIterator = destUids.iterator();
                                    uidMap = new HashMap<String, String>();
                                    while (srcUidsIterator.hasNext() &&
                                            destUidsIterator.hasNext()) {
                                        String srcUid = srcUidsIterator.next();
                                        String destUid = destUidsIterator.next();
                                        uidMap.put(srcUid, destUid);
                                    }
                                } else {
                                    if (K9MailLib.isDebug()) {
                                        Log.v(LOG_TAG, "Parse error: size of source UIDs " +
                                                "list is not the same as size of destination " +
                                                "UIDs list.");
                                    }
                                }
                            } else {
                                if (K9MailLib.isDebug()) {
                                    Log.v(LOG_TAG, "Parsing of the sequence set failed.");
                                }
                            }
                        }
                    }
                }

                return uidMap;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public Map<String, String> moveMessages(List<? extends Message> messages, Folder folder) throws MessagingException {
            if (messages.isEmpty())
                return null;
            Map<String, String> uidMap = copyMessages(messages, folder);
            setFlags(messages, Collections.singleton(Flag.DELETED), true);
            return uidMap;
        }

        @Override
        public void delete(List<? extends Message> messages, String trashFolderName) throws MessagingException {
            if (messages.isEmpty())
                return;

            if (trashFolderName == null || getName().equalsIgnoreCase(trashFolderName)) {
                setFlags(messages, Collections.singleton(Flag.DELETED), true);
            } else {
                ImapFolder remoteTrashFolder = (ImapFolder)getStore().getFolder(trashFolderName);
                String remoteTrashName = encodeString(encodeFolderName(remoteTrashFolder.getPrefixedName()));

                if (!exists(remoteTrashName)) {
                    /*
                     * If the remote trash folder doesn't exist we try to create it.
                     */
                    if (K9MailLib.isDebug())
                        Log.i(LOG_TAG, "IMAPMessage.delete: attempting to create remote '" + trashFolderName + "' folder for " + getLogId());
                    remoteTrashFolder.create(FolderType.HOLDS_MESSAGES);
                }

                if (exists(remoteTrashName)) {
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "IMAPMessage.delete: copying remote " + messages.size() + " messages to '" + trashFolderName + "' for " + getLogId());

                    moveMessages(messages, remoteTrashFolder);
                } else {
                    throw new MessagingException("IMAPMessage.delete: remote Trash folder " + trashFolderName + " does not exist and could not be created for " + getLogId()
                                                 , true);
                }
            }
        }


        @Override
        public int getMessageCount() {
            return mMessageCount;
        }


        private int getRemoteMessageCount(String criteria) throws MessagingException {
            checkOpen(); //only need READ access
            try {
                int count = 0;
                int start = 1;

                List<ImapResponse> responses = executeSimpleCommand(String.format(Locale.US, "SEARCH %d:* %s", start, criteria));
                for (ImapResponse response : responses) {
                    if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                        count += response.size() - 1;
                    }
                }
                return count;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }


        }

        @Override
        public int getUnreadMessageCount() throws MessagingException {
            return getRemoteMessageCount("UNSEEN NOT DELETED");
        }

        @Override
        public int getFlaggedMessageCount() throws MessagingException {
            return getRemoteMessageCount("FLAGGED NOT DELETED");
        }

        protected long getHighestUid() {
            try {
                ImapSearcher searcher = new ImapSearcher() {
                    @Override
                    public List<ImapResponse> search() throws IOException, MessagingException {
                        return executeSimpleCommand("UID SEARCH *:*");
                    }
                };
                List<? extends Message> messages = search(searcher, null);
                if (messages.size() > 0) {
                    return Long.parseLong(messages.get(0).getUid());
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to find highest UID in folder " + getName(), e);
            }
            return -1L;

        }

        @Override
        public void delete(boolean recurse) throws MessagingException {
            throw new Error("ImapStore.delete() not yet implemented");
        }

        @Override
        public ImapMessage getMessage(String uid) throws MessagingException {
            return new ImapMessage(uid, this);
        }


        @Override
        public List<ImapMessage> getMessages(int start, int end, Date earliestDate, MessageRetrievalListener<ImapMessage> listener)
        throws MessagingException {
            return getMessages(start, end, earliestDate, false, listener);
        }

        protected List<ImapMessage> getMessages(final int start, final int end, Date earliestDate, final boolean includeDeleted, final MessageRetrievalListener<ImapMessage> listener)
        throws MessagingException {
            if (start < 1 || end < 1 || end < start) {
                throw new MessagingException(
                    String.format(Locale.US, "Invalid message set %d %d",
                                  start, end));
            }
            final StringBuilder dateSearchString = new StringBuilder();
            if (earliestDate != null) {
                dateSearchString.append(" SINCE ");
                synchronized (RFC3501_DATE) {
                    dateSearchString.append(RFC3501_DATE.format(earliestDate));
                }
            }


            ImapSearcher searcher = new ImapSearcher() {
                @Override
                public List<ImapResponse> search() throws IOException, MessagingException {
                    return executeSimpleCommand(String.format(Locale.US, "UID SEARCH %d:%d%s%s", start, end, dateSearchString, includeDeleted ? "" : " NOT DELETED"));
                }
            };
            return search(searcher, listener);

        }
        protected List<ImapMessage> getMessages(final List<Long> mesgSeqs,
                                                      final boolean includeDeleted,
                                                      final MessageRetrievalListener<ImapMessage> listener)
        throws MessagingException {
            ImapSearcher searcher = new ImapSearcher() {
                @Override
                public List<ImapResponse> search() throws IOException, MessagingException {
                    return executeSimpleCommand(String.format("UID SEARCH %s%s", combine(mesgSeqs.toArray(), ','), includeDeleted ? "" : " NOT DELETED"));
                }
            };
            return search(searcher, listener);
        }

        protected List<? extends Message> getMessagesFromUids(final List<String> mesgUids,
                                                              final boolean includeDeleted,
                                                              final MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
            ImapSearcher searcher = new ImapSearcher() {
                @Override
                public List<ImapResponse> search() throws IOException, MessagingException {
                    return executeSimpleCommand(String.format("UID SEARCH UID %s%s", combine(mesgUids.toArray(), ','), includeDeleted ? "" : " NOT DELETED"));
                }
            };
            return search(searcher, listener);
        }

        protected List<ImapMessage> search(ImapSearcher searcher, MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
            checkOpen(); //only need READ access
            List<ImapMessage> messages = new ArrayList<ImapMessage>();
            try {
                List<Long> uids = new ArrayList<Long>();
                List<ImapResponse> responses = searcher.search(); //
                for (ImapResponse response : responses) {
                    if (response.getTag() == null) {
                        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                            for (int i = 1, count = response.size(); i < count; i++) {
                                uids.add(response.getLong(i));
                            }
                        }
                    }
                }

                // Sort the uids in numerically decreasing order
                // By doing it in decreasing order, we ensure newest messages are dealt with first
                // This makes the most sense when a limit is imposed, and also prevents UI from going
                // crazy adding stuff at the top.
                Collections.sort(uids, Collections.reverseOrder());

                for (int i = 0, count = uids.size(); i < count; i++) {
                    String uid = uids.get(i).toString();
                    if (listener != null) {
                        listener.messageStarted(uid, i, count);
                    }
                    ImapMessage message = new ImapMessage(uid, this);
                    messages.add(message);
                    if (listener != null) {
                        listener.messageFinished(message, i, count);
                    }
                }
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return messages;
        }


        @Override
        public List<ImapMessage> getMessages(MessageRetrievalListener<ImapMessage> listener) throws MessagingException {
            return getMessages(null, listener);
        }

        @Override
        public List<ImapMessage> getMessages(String[] uids, MessageRetrievalListener<ImapMessage> listener)
        throws MessagingException {
            checkOpen(); //only need READ access
            List<ImapMessage> messages = new ArrayList<ImapMessage>();
            try {
                if (uids == null) {
                    List<ImapResponse> responses = executeSimpleCommand("UID SEARCH 1:* NOT DELETED");
                    List<String> tempUids = new ArrayList<String>();
                    for (ImapResponse response : responses) {
                        if (ImapResponseParser.equalsIgnoreCase(response.get(0), "SEARCH")) {
                            for (int i = 1, count = response.size(); i < count; i++) {
                                tempUids.add(response.getString(i));
                            }
                        }
                    }
                    uids = tempUids.toArray(EMPTY_STRING_ARRAY);
                }
                for (int i = 0, count = uids.length; i < count; i++) {
                    if (listener != null) {
                        listener.messageStarted(uids[i], i, count);
                    }
                    ImapMessage message = new ImapMessage(uids[i], this);
                    messages.add(message);
                    if (listener != null) {
                        listener.messageFinished(message, i, count);
                    }
                }
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
            return messages;
        }

        @Override
        public void fetch(List<ImapMessage> messages, FetchProfile fp, MessageRetrievalListener<ImapMessage> listener)
        throws MessagingException {
            if (messages == null || messages.isEmpty()) {
                return;
            }
            checkOpen(); //only need READ access
            List<String> uids = new ArrayList<String>(messages.size());
            HashMap<String, Message> messageMap = new HashMap<String, Message>();
            for (Message msg : messages) {
                String uid = msg.getUid();
                uids.add(uid);
                messageMap.put(uid, msg);
            }

            /*
             * Figure out what command we are going to run:
             * Flags - UID FETCH (FLAGS)
             * Envelope - UID FETCH ([FLAGS] INTERNALDATE UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc)])
             *
             */
            Set<String> fetchFields = new LinkedHashSet<String>();
            fetchFields.add("UID");
            if (fp.contains(FetchProfile.Item.FLAGS)) {
                fetchFields.add("FLAGS");
            }
            if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                fetchFields.add("INTERNALDATE");
                fetchFields.add("RFC822.SIZE");
                fetchFields.add("BODY.PEEK[HEADER.FIELDS (date subject from content-type to cc " +
                        "reply-to message-id references in-reply-to " + K9MailLib.IDENTITY_HEADER + ")]");
            }
            if (fp.contains(FetchProfile.Item.STRUCTURE)) {
                fetchFields.add("BODYSTRUCTURE");
            }
            if (fp.contains(FetchProfile.Item.BODY_SANE)) {
                // If the user wants to download unlimited-size messages, don't go only for the truncated body
                if (mStoreConfig.getMaximumAutoDownloadMessageSize() > 0) {
                    fetchFields.add(String.format(Locale.US, "BODY.PEEK[]<0.%d>", mStoreConfig.getMaximumAutoDownloadMessageSize()));
                } else {
                    fetchFields.add("BODY.PEEK[]");
                }
            }
            if (fp.contains(FetchProfile.Item.BODY)) {
                fetchFields.add("BODY.PEEK[]");
            }



            for (int windowStart = 0; windowStart < messages.size(); windowStart += (FETCH_WINDOW_SIZE)) {
                List<String> uidWindow = uids.subList(windowStart, Math.min((windowStart + FETCH_WINDOW_SIZE), messages.size()));

                try {
                    mConnection.sendCommand(String.format("UID FETCH %s (%s)",
                                                          combine(uidWindow.toArray(new String[uidWindow.size()]), ','),
                                                          combine(fetchFields.toArray(new String[fetchFields.size()]), ' ')
                                                         ), false);
                    ImapResponse response;
                    int messageNumber = 0;

                    ImapResponseCallback callback = null;
                    if (fp.contains(FetchProfile.Item.BODY) || fp.contains(FetchProfile.Item.BODY_SANE)) {
                        callback = new FetchBodyCallback(messageMap);
                    }

                    do {
                        response = mConnection.readResponse(callback);

                        if (response.getTag() == null && ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
                            ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                            String uid = fetchList.getKeyedString("UID");
                            long msgSeq = response.getLong(0);
                            if (uid != null) {
                                try {
                                    msgSeqUidMap.put(msgSeq, uid);
                                    if (K9MailLib.isDebug()) {
                                        Log.v(LOG_TAG, "Stored uid '" + uid + "' for msgSeq " + msgSeq + " into map " /*+ msgSeqUidMap.toString() */);
                                    }
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "Unable to store uid '" + uid + "' for msgSeq " + msgSeq);
                                }
                            }

                            Message message = messageMap.get(uid);
                            if (message == null) {
                                if (K9MailLib.isDebug())
                                    Log.d(LOG_TAG, "Do not have message in messageMap for UID " + uid + " for " + getLogId());

                                handleUntaggedResponse(response);
                                continue;
                            }
                            if (listener != null) {
                                listener.messageStarted(uid, messageNumber++, messageMap.size());
                            }

                            ImapMessage imapMessage = (ImapMessage) message;

                            Object literal = handleFetchResponse(imapMessage, fetchList);

                            if (literal != null) {
                                if (literal instanceof String) {
                                    String bodyString = (String)literal;
                                    InputStream bodyStream = new ByteArrayInputStream(bodyString.getBytes());
                                    imapMessage.parse(bodyStream);
                                } else if (literal instanceof Integer) {
                                    // All the work was done in FetchBodyCallback.foundLiteral()
                                } else {
                                    // This shouldn't happen
                                    throw new MessagingException("Got FETCH response with bogus parameters");
                                }
                            }

                            if (listener != null) {
                                listener.messageFinished(imapMessage, messageNumber, messageMap.size());
                            }
                        } else {
                            handleUntaggedResponse(response);
                        }

                    } while (response.getTag() == null);
                } catch (IOException ioe) {
                    throw ioExceptionHandler(mConnection, ioe);
                }
            }
        }


        @Override
        public void fetchPart(Message message, Part part, MessageRetrievalListener<Message> listener)
        throws MessagingException {
            checkOpen(); //only need READ access

            String partId = part.getServerExtra();

            String fetch;
            if ("TEXT".equalsIgnoreCase(partId)) {
                fetch = String.format(Locale.US, "BODY.PEEK[TEXT]<0.%d>",
                        mStoreConfig.getMaximumAutoDownloadMessageSize());
            } else {
                fetch = String.format("BODY.PEEK[%s]", partId);
            }

            try {
                mConnection.sendCommand(
                    String.format("UID FETCH %s (UID %s)", message.getUid(), fetch),
                    false);

                ImapResponse response;
                int messageNumber = 0;

                ImapResponseCallback callback = new FetchPartCallback(part);

                do {
                    response = mConnection.readResponse(callback);

                    if ((response.getTag() == null) &&
                            (ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH"))) {
                        ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                        String uid = fetchList.getKeyedString("UID");

                        if (!message.getUid().equals(uid)) {
                            if (K9MailLib.isDebug())
                                Log.d(LOG_TAG, "Did not ask for UID " + uid + " for " + getLogId());

                            handleUntaggedResponse(response);
                            continue;
                        }
                        if (listener != null) {
                            listener.messageStarted(uid, messageNumber++, 1);
                        }

                        ImapMessage imapMessage = (ImapMessage) message;

                        Object literal = handleFetchResponse(imapMessage, fetchList);

                        if (literal != null) {
                            if (literal instanceof Body) {
                                // Most of the work was done in FetchAttchmentCallback.foundLiteral()
                                MimeMessageHelper.setBody(part, (Body) literal);
                            } else if (literal instanceof String) {
                                String bodyString = (String)literal;
                                InputStream bodyStream = new ByteArrayInputStream(bodyString.getBytes());

                                String contentTransferEncoding = part
                                        .getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                                String contentType = part
                                        .getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0];
                                MimeMessageHelper.setBody(part, MimeUtility.createBody(bodyStream,
                                        contentTransferEncoding, contentType));
                            } else {
                                // This shouldn't happen
                                throw new MessagingException("Got FETCH response with bogus parameters");
                            }
                        }

                        if (listener != null) {
                            listener.messageFinished(message, messageNumber, 1);
                        }
                    } else {
                        handleUntaggedResponse(response);
                    }

                } while (response.getTag() == null);
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        // Returns value of body field
        private Object handleFetchResponse(ImapMessage message, ImapList fetchList) throws MessagingException {
            Object result = null;
            if (fetchList.containsKey("FLAGS")) {
                ImapList flags = fetchList.getKeyedList("FLAGS");
                if (flags != null) {
                    for (int i = 0, count = flags.size(); i < count; i++) {
                        String flag = flags.getString(i);
                        if (flag.equalsIgnoreCase("\\Deleted")) {
                            message.setFlagInternal(Flag.DELETED, true);
                        } else if (flag.equalsIgnoreCase("\\Answered")) {
                            message.setFlagInternal(Flag.ANSWERED, true);
                        } else if (flag.equalsIgnoreCase("\\Seen")) {
                            message.setFlagInternal(Flag.SEEN, true);
                        } else if (flag.equalsIgnoreCase("\\Flagged")) {
                            message.setFlagInternal(Flag.FLAGGED, true);
                        } else if (flag.equalsIgnoreCase("$Forwarded")) {
                            message.setFlagInternal(Flag.FORWARDED, true);
                            /* a message contains FORWARDED FLAG -> so we can also create them */
                            mPermanentFlagsIndex.add(Flag.FORWARDED);
                        }
                    }
                }
            }

            if (fetchList.containsKey("INTERNALDATE")) {
                Date internalDate = fetchList.getKeyedDate("INTERNALDATE");
                message.setInternalDate(internalDate);
            }

            if (fetchList.containsKey("RFC822.SIZE")) {
                int size = fetchList.getKeyedNumber("RFC822.SIZE");
                message.setSize(size);
            }

            if (fetchList.containsKey("BODYSTRUCTURE")) {
                ImapList bs = fetchList.getKeyedList("BODYSTRUCTURE");
                if (bs != null) {
                    try {
                        parseBodyStructure(bs, message, "TEXT");
                    } catch (MessagingException e) {
                        if (K9MailLib.isDebug())
                            Log.d(LOG_TAG, "Error handling message for " + getLogId(), e);
                        message.setBody(null);
                    }
                }
            }

            if (fetchList.containsKey("BODY")) {
                int index = fetchList.getKeyIndex("BODY") + 2;
                int size = fetchList.size();
                if (index < size) {
                    result = fetchList.getObject(index);

                    // Check if there's an origin octet
                    if (result instanceof String) {
                        String originOctet = (String) result;
                        if (originOctet.startsWith("<") && (index + 1) < size) {
                            result = fetchList.getObject(index + 1);
                        }
                    }
                }
            }

            return result;
        }

        /**
         * Handle any untagged responses that the caller doesn't care to handle themselves.
         */
        protected List<ImapResponse> handleUntaggedResponses(List<ImapResponse> responses) {
            for (ImapResponse response : responses) {
                handleUntaggedResponse(response);
            }
            return responses;
        }

        protected void handlePossibleUidNext(ImapResponse response) {
            if (ImapResponseParser.equalsIgnoreCase(response.get(0), "OK") && response.size() > 1) {
                Object bracketedObj = response.get(1);
                if (bracketedObj instanceof ImapList) {
                    ImapList bracketed = (ImapList)bracketedObj;

                    if (bracketed.size() > 1) {
                        Object keyObj = bracketed.get(0);
                        if (keyObj instanceof String) {
                            String key = (String)keyObj;
                            if ("UIDNEXT".equalsIgnoreCase(key)) {
                                uidNext = bracketed.getLong(1);
                                if (K9MailLib.isDebug())
                                    Log.d(LOG_TAG, "Got UidNext = " + uidNext + " for " + getLogId());
                            }
                        }
                    }


                }
            }
        }

        /**
         * Handle an untagged response that the caller doesn't care to handle themselves.
         */
        protected void handleUntaggedResponse(ImapResponse response) {
            if (response.getTag() == null && response.size() > 1) {
                if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXISTS")) {
                    mMessageCount = response.getNumber(0);
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "Got untagged EXISTS with value " + mMessageCount + " for " + getLogId());
                }
                handlePossibleUidNext(response);

                if (ImapResponseParser.equalsIgnoreCase(response.get(1), "EXPUNGE") && mMessageCount > 0) {
                    mMessageCount--;
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "Got untagged EXPUNGE with mMessageCount " + mMessageCount + " for " + getLogId());
                }
//            if (response.size() > 1) {
//                Object bracketedObj = response.get(1);
//                if (bracketedObj instanceof ImapList)
//                {
//                    ImapList bracketed = (ImapList)bracketedObj;
//
//                    if (!bracketed.isEmpty())
//                    {
//                        Object keyObj = bracketed.get(0);
//                        if (keyObj instanceof String)
//                        {
//                            String key = (String)keyObj;
//                            if ("ALERT".equalsIgnoreCase(key))
//                            {
//                                StringBuilder sb = new StringBuilder();
//                                for (int i = 2, count = response.size(); i < count; i++) {
//                                    sb.append(response.get(i).toString());
//                                    sb.append(' ');
//                                }
//
//                                Log.w(LOG_TAG, "ALERT: " + sb.toString() + " for " + getLogId());
//                            }
//                        }
//                    }
//
//
//                }
//            }
            }
            //Log.i(LOG_TAG, "mMessageCount = " + mMessageCount + " for " + getLogId());
        }

        private void parseBodyStructure(ImapList bs, Part part, String id)
        throws MessagingException {
            if (bs.get(0) instanceof ImapList) {
                /*
                 * This is a multipart/*
                 */
                MimeMultipart mp = new MimeMultipart();
                for (int i = 0, count = bs.size(); i < count; i++) {
                    if (bs.get(i) instanceof ImapList) {
                        /*
                         * For each part in the message we're going to add a new BodyPart and parse
                         * into it.
                         */
                        MimeBodyPart bp = new MimeBodyPart();
                        if (id.equalsIgnoreCase("TEXT")) {
                            parseBodyStructure(bs.getList(i), bp, Integer.toString(i + 1));
                        } else {
                            parseBodyStructure(bs.getList(i), bp, id + "." + (i + 1));
                        }
                        mp.addBodyPart(bp);
                    } else {
                        /*
                         * We've got to the end of the children of the part, so now we can find out
                         * what type it is and bail out.
                         */
                        String subType = bs.getString(i);
                        mp.setSubType(subType.toLowerCase(Locale.US));
                        break;
                    }
                }
                MimeMessageHelper.setBody(part, mp);
            } else {
                /*
                 * This is a body. We need to add as much information as we can find out about
                 * it to the Part.
                 */

                /*
                 *  0| 0  body type
                 *  1| 1  body subtype
                 *  2| 2  body parameter parenthesized list
                 *  3| 3  body id (unused)
                 *  4| 4  body description (unused)
                 *  5| 5  body encoding
                 *  6| 6  body size
                 *  -| 7  text lines (only for type TEXT, unused)
                 * Extensions (optional):
                 *  7| 8  body MD5 (unused)
                 *  8| 9  body disposition
                 *  9|10  body language (unused)
                 * 10|11  body location (unused)
                 */

                String type = bs.getString(0);
                String subType = bs.getString(1);
                String mimeType = (type + "/" + subType).toLowerCase(Locale.US);

                ImapList bodyParams = null;
                if (bs.get(2) instanceof ImapList) {
                    bodyParams = bs.getList(2);
                }
                String encoding = bs.getString(5);
                int size = bs.getNumber(6);

                if (MimeUtility.mimeTypeMatches(mimeType, "message/rfc822")) {
//                  A body type of type MESSAGE and subtype RFC822
//                  contains, immediately after the basic fields, the
//                  envelope structure, body structure, and size in
//                  text lines of the encapsulated message.
//                    [MESSAGE, RFC822, [NAME, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory allocation - displayware.eml], NIL, NIL, 7BIT, 5974, NIL, [INLINE, [FILENAME*0, Fwd: [#HTR-517941]:  update plans at 1am Friday - Memory all, FILENAME*1, ocation - displayware.eml]], NIL]
                    /*
                     * This will be caught by fetch and handled appropriately.
                     */
                    throw new MessagingException("BODYSTRUCTURE message/rfc822 not yet supported.");
                }

                /*
                 * Set the content type with as much information as we know right now.
                 */
                StringBuilder contentType = new StringBuilder();
                contentType.append(mimeType);

                if (bodyParams != null) {
                    /*
                     * If there are body params we might be able to get some more information out
                     * of them.
                     */
                    for (int i = 0, count = bodyParams.size(); i < count; i += 2) {
                        contentType.append(String.format(";\r\n %s=\"%s\"",
                                           bodyParams.getString(i),
                                           bodyParams.getString(i + 1)));
                    }
                }

                part.setHeader(MimeHeader.HEADER_CONTENT_TYPE, contentType.toString());

                // Extension items
                ImapList bodyDisposition = null;
                if (("text".equalsIgnoreCase(type))
                        && (bs.size() > 9)
                        && (bs.get(9) instanceof ImapList)) {
                    bodyDisposition = bs.getList(9);
                } else if (!("text".equalsIgnoreCase(type))
                           && (bs.size() > 8)
                           && (bs.get(8) instanceof ImapList)) {
                    bodyDisposition = bs.getList(8);
                }

                StringBuilder contentDisposition = new StringBuilder();

                if (bodyDisposition != null && !bodyDisposition.isEmpty()) {
                    if (!"NIL".equalsIgnoreCase(bodyDisposition.getString(0))) {
                        contentDisposition.append(bodyDisposition.getString(0).toLowerCase(Locale.US));
                    }

                    if ((bodyDisposition.size() > 1)
                            && (bodyDisposition.get(1) instanceof ImapList)) {
                        ImapList bodyDispositionParams = bodyDisposition.getList(1);
                        /*
                         * If there is body disposition information we can pull some more information
                         * about the attachment out.
                         */
                        for (int i = 0, count = bodyDispositionParams.size(); i < count; i += 2) {
                            contentDisposition.append(String.format(";\r\n %s=\"%s\"",
                                                      bodyDispositionParams.getString(i).toLowerCase(Locale.US),
                                                      bodyDispositionParams.getString(i + 1)));
                        }
                    }
                }

                if (MimeUtility.getHeaderParameter(contentDisposition.toString(), "size") == null) {
                    contentDisposition.append(String.format(Locale.US, ";\r\n size=%d", size));
                }

                /*
                 * Set the content disposition containing at least the size. Attachment
                 * handling code will use this down the road.
                 */
                part.setHeader(MimeHeader.HEADER_CONTENT_DISPOSITION, contentDisposition.toString());


                /*
                 * Set the Content-Transfer-Encoding header. Attachment code will use this
                 * to parse the body.
                 */
                part.setHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING, encoding);

                if (part instanceof ImapMessage) {
                    ((ImapMessage) part).setSize(size);
                }
                part.setServerExtra(id);
            }

        }

        /**
         * Appends the given messages to the selected folder.
         *
         * <p>
         * This implementation also determines the new UIDs of the given messages on the IMAP
         * server and changes the messages' UIDs to the new server UIDs.
         * </p>
         *
         * @param messages
         *         The messages to append to the folder.
         *
         * @return The mapping of original message UIDs to the new server UIDs.
         */
        @Override
        public Map<String, String> appendMessages(List<? extends Message> messages) throws MessagingException {
            open(OPEN_MODE_RW);
            checkOpen();
            try {
                Map<String, String> uidMap = new HashMap<String, String>();
                for (Message message : messages) {
                    mConnection.sendCommand(
                        String.format(Locale.US, "APPEND %s (%s) {%d}",
                                      encodeString(encodeFolderName(getPrefixedName())),
                                      combineFlags(message.getFlags()),
                                      message.calculateSize()), false);

                    ImapResponse response;
                    do {
                        response = mConnection.readResponse();
                        handleUntaggedResponse(response);
                        if (response.isContinuationRequested()) {
                            EOLConvertingOutputStream eolOut = new EOLConvertingOutputStream(mConnection.getOutputStream());
                            message.writeTo(eolOut);
                            eolOut.write('\r');
                            eolOut.write('\n');
                            eolOut.flush();
                        }
                    } while (response.getTag() == null);

                    if (response.size() > 1) {
                        /*
                         * If the server supports UIDPLUS, then along with the APPEND response it
                         * will return an APPENDUID response code, e.g.
                         *
                         * 11 OK [APPENDUID 2 238268] APPEND completed
                         *
                         * We can use the UID included in this response to update our records.
                         */
                        Object responseList = response.get(1);

                        if (responseList instanceof ImapList) {
                            ImapList appendList = (ImapList) responseList;
                            if (appendList.size() >= 3 &&
                                    appendList.getString(0).equals("APPENDUID")) {

                                String newUid = appendList.getString(2);

                                if (!TextUtils.isEmpty(newUid)) {
                                    message.setUid(newUid);
                                    uidMap.put(message.getUid(), newUid);
                                    continue;
                                }
                            }
                        }
                    }

                    /*
                     * This part is executed in case the server does not support UIDPLUS or does
                     * not implement the APPENDUID response code.
                     */
                    String newUid = getUidFromMessageId(message);
                    if (K9MailLib.isDebug()) {
                        Log.d(LOG_TAG, "Got UID " + newUid + " for message for " + getLogId());
                    }

                    if (!TextUtils.isEmpty(newUid)) {
                        uidMap.put(message.getUid(), newUid);
                        message.setUid(newUid);
                    }
                }

                /*
                 * We need uidMap to be null if new UIDs are not available to maintain consistency
                 * with the behavior of other similar methods (copyMessages, moveMessages) which
                 * return null.
                 */
                return (uidMap.isEmpty()) ? null : uidMap;
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public String getUidFromMessageId(Message message) throws MessagingException {
            try {
                /*
                * Try to find the UID of the message we just appended using the
                * Message-ID header.
                */
                String[] messageIdHeader = message.getHeader("Message-ID");

                if (messageIdHeader.length == 0) {
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "Did not get a message-id in order to search for UID  for " + getLogId());
                    return null;
                }
                String messageId = messageIdHeader[0];
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "Looking for UID for message with message-id " + messageId + " for " + getLogId());

                List<ImapResponse> responses =
                    executeSimpleCommand(
                        String.format("UID SEARCH HEADER MESSAGE-ID %s", encodeString(messageId)));
                for (ImapResponse response1 : responses) {
                    if (response1.getTag() == null && ImapResponseParser.equalsIgnoreCase(response1.get(0), "SEARCH")
                            && response1.size() > 1) {
                        return response1.getString(1);
                    }
                }
                return null;
            } catch (IOException ioe) {
                throw new MessagingException("Could not find UID for message based on Message-ID", ioe);
            }
        }


        @Override
        public void expunge() throws MessagingException {
            open(OPEN_MODE_RW);
            checkOpen();
            try {
                executeSimpleCommand("EXPUNGE");
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        private String combineFlags(Iterable<Flag> flags) {
            List<String> flagNames = new ArrayList<String>();
            for (Flag flag : flags) {
                if (flag == Flag.SEEN) {
                    flagNames.add("\\Seen");
                } else if (flag == Flag.DELETED) {
                    flagNames.add("\\Deleted");
                } else if (flag == Flag.ANSWERED) {
                    flagNames.add("\\Answered");
                } else if (flag == Flag.FLAGGED) {
                    flagNames.add("\\Flagged");
                } else if (flag == Flag.FORWARDED
                        && (mCanCreateKeywords || mPermanentFlagsIndex.contains(Flag.FORWARDED))) {
                    flagNames.add("$Forwarded");
                }

            }
            return combine(flagNames.toArray(new String[flagNames.size()]), ' ');
        }


        @Override
        public void setFlags(Set<Flag> flags, boolean value)
        throws MessagingException {
            open(OPEN_MODE_RW);
            checkOpen();


            try {
                executeSimpleCommand(String.format("UID STORE 1:* %sFLAGS.SILENT (%s)",
                                                   value ? "+" : "-", combineFlags(flags)));
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        @Override
        public String getNewPushState(String oldPushStateS, Message message) {
            try {
                String messageUidS = message.getUid();
                long messageUid = Long.parseLong(messageUidS);
                ImapPushState oldPushState = ImapPushState.parse(oldPushStateS);
                if (messageUid >= oldPushState.uidNext) {
                    long uidNext = messageUid + 1;
                    ImapPushState newPushState = new ImapPushState(uidNext);
                    return newPushState.toString();
                } else {
                    return null;
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception while updated push state for " + getLogId(), e);
                return null;
            }
        }


        @Override
        public void setFlags(List<? extends Message> messages, final Set<Flag> flags, boolean value)
        throws MessagingException {
            open(OPEN_MODE_RW);
            checkOpen();
            String[] uids = new String[messages.size()];
            for (int i = 0, count = messages.size(); i < count; i++) {
                uids[i] = messages.get(i).getUid();
            }
            try {
                executeSimpleCommand(String.format("UID STORE %s %sFLAGS.SILENT (%s)",
                                                   combine(uids, ','),
                                                   value ? "+" : "-",
                                                   combineFlags(flags)));
            } catch (IOException ioe) {
                throw ioExceptionHandler(mConnection, ioe);
            }
        }

        private void checkOpen() throws MessagingException {
            if (!isOpen()) {
                throw new MessagingException("Folder " + getPrefixedName() + " is not open.");
            }
        }

        private MessagingException ioExceptionHandler(ImapConnection connection, IOException ioe) {
            Log.e(LOG_TAG, "IOException for " + getLogId(), ioe);
            if (connection != null) {
                connection.close();
            }
            close();
            return new MessagingException("IO Error", ioe);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ImapFolder) {
                return ((ImapFolder)o).getName().equalsIgnoreCase(getName());
            }
            return super.equals(o);
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        protected ImapStore getStore() {
            return store;
        }

        protected String getLogId() {
            String id = mStoreConfig.toString() + ":" + getName() + "/" + Thread.currentThread().getName();
            if (mConnection != null) {
                id += "/" + mConnection.getLogId();
            }
            return id;
        }

        /**
         * Search the remote ImapFolder.
         * @param queryString String to query for.
         * @param requiredFlags Mandatory flags
         * @param forbiddenFlags Flags to exclude
         * @return List of messages found
         * @throws MessagingException On any error.
         */
        @Override
        public List<ImapMessage> search(final String queryString, final Set<Flag> requiredFlags, final Set<Flag> forbiddenFlags)
            throws MessagingException {

            if (!mStoreConfig.allowRemoteSearch()) {
                throw new MessagingException("Your settings do not allow remote searching of this account");
            }

            // Setup the searcher
            final ImapSearcher searcher = new ImapSearcher() {
                @Override
                public List<ImapResponse> search() throws IOException, MessagingException {
                    String imapQuery = "UID SEARCH ";
                    if (requiredFlags != null) {
                        for (Flag f : requiredFlags) {
                            switch (f) {
                                case DELETED:
                                    imapQuery += "DELETED ";
                                    break;

                                case SEEN:
                                    imapQuery += "SEEN ";
                                    break;

                                case ANSWERED:
                                    imapQuery += "ANSWERED ";
                                    break;

                                case FLAGGED:
                                    imapQuery += "FLAGGED ";
                                    break;

                                case DRAFT:
                                    imapQuery += "DRAFT ";
                                    break;

                                case RECENT:
                                    imapQuery += "RECENT ";
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                    if (forbiddenFlags != null) {
                        for (Flag f : forbiddenFlags) {
                            switch (f) {
                                case DELETED:
                                    imapQuery += "UNDELETED ";
                                    break;

                                case SEEN:
                                    imapQuery += "UNSEEN ";
                                    break;

                                case ANSWERED:
                                    imapQuery += "UNANSWERED ";
                                    break;

                                case FLAGGED:
                                    imapQuery += "UNFLAGGED ";
                                    break;

                                case DRAFT:
                                    imapQuery += "UNDRAFT ";
                                    break;

                                case RECENT:
                                    imapQuery += "UNRECENT ";
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                    final String encodedQry = encodeString(queryString);
                    if (mStoreConfig.isRemoteSearchFullText()) {
                        imapQuery += "TEXT " + encodedQry;
                    } else {
                        imapQuery += "OR SUBJECT " + encodedQry + " FROM " + encodedQry;
                    }
                    return executeSimpleCommand(imapQuery);
                }
            };

            // Execute the search
            try {
                open(OPEN_MODE_RO);
                checkOpen();

                mInSearch = true;
                // don't pass listener--we don't want to add messages until we've downloaded them
                return search(searcher, null);
            } finally {
                mInSearch = false;
            }

        }
    }

    protected static class ImapMessage extends MimeMessage {
        ImapMessage(String uid, Folder folder) {
            this.mUid = uid;
            this.mFolder = folder;
        }

        public void setSize(int size) {
            this.mSize = size;
        }

        public void setFlagInternal(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
        }


        @Override
        public void setFlag(Flag flag, boolean set) throws MessagingException {
            super.setFlag(flag, set);
            mFolder.setFlags(Collections.singletonList(this), Collections.singleton(flag), set);
        }

        @Override
        public void delete(String trashFolderName) throws MessagingException {
            getFolder().delete(Collections.singletonList(this), trashFolderName);
        }
    }

    protected class ImapFolderPusher extends ImapFolder implements UntaggedHandler {
        private final PushReceiver receiver;
        private Thread listeningThread = null;
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final AtomicBoolean idling = new AtomicBoolean(false);
        private final AtomicBoolean doneSent = new AtomicBoolean(false);
        private final AtomicInteger delayTime = new AtomicInteger(NORMAL_DELAY_TIME);
        private final AtomicInteger idleFailureCount = new AtomicInteger(0);
        private final AtomicBoolean needsPoll = new AtomicBoolean(false);
        private List<ImapResponse> storedUntaggedResponses = new ArrayList<ImapResponse>();
        private TracingWakeLock wakeLock = null;

        public ImapFolderPusher(ImapStore store, String name, PushReceiver nReceiver) {
            super(store, name);
            receiver = nReceiver;
            TracingPowerManager pm = TracingPowerManager.getPowerManager(receiver.getContext());
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ImapFolderPusher " + mStoreConfig.toString() + ":" + getName());
            wakeLock.setReferenceCounted(false);

        }
        public void refresh() throws IOException, MessagingException {
            if (idling.get()) {
                wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
                sendDone();
            }
        }

        private void sendDone() throws IOException, MessagingException {
            if (doneSent.compareAndSet(false, true)) {
                ImapConnection conn = mConnection;
                if (conn != null) {
                    conn.setReadTimeout(SOCKET_READ_TIMEOUT);
                    sendContinuation("DONE");
                }

            }
        }

        private void sendContinuation(String continuation)
        throws IOException {
            ImapConnection conn = mConnection;
            if (conn != null) {
                conn.sendContinuation(continuation);
            }
        }

        public void start() {
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
                    if (K9MailLib.isDebug())
                        Log.i(LOG_TAG, "Pusher starting for " + getLogId());

                    long lastUidNext = -1L;
                    while (!stop.get()) {
                        try {
                            long oldUidNext = -1L;
                            try {
                                String pushStateS = receiver.getPushState(getName());
                                ImapPushState pushState = ImapPushState.parse(pushStateS);
                                oldUidNext = pushState.uidNext;
                                if (K9MailLib.isDebug())
                                    Log.i(LOG_TAG, "Got oldUidNext " + oldUidNext + " for " + getLogId());
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Unable to get oldUidNext for " + getLogId(), e);
                            }

                            /*
                             * This makes sure 'oldUidNext' is never smaller than 'UIDNEXT' from
                             * the last loop iteration. This way we avoid looping endlessly causing
                             * the battery to drain.
                             *
                             * See issue 4907
                             */
                            if (oldUidNext < lastUidNext) {
                                oldUidNext = lastUidNext;
                            }

                            ImapConnection oldConnection = mConnection;
                            internalOpen(OPEN_MODE_RO);
                            ImapConnection conn = mConnection;
                            if (conn == null) {
                                receiver.pushError("Could not establish connection for IDLE", null);
                                throw new MessagingException("Could not establish connection for IDLE");

                            }
                            if (!conn.isIdleCapable()) {
                                stop.set(true);
                                receiver.pushError("IMAP server is not IDLE capable: " + conn.toString(), null);
                                throw new MessagingException("IMAP server is not IDLE capable:" + conn.toString());
                            }

                            if (!stop.get() && mStoreConfig.isPushPollOnConnect() && (conn != oldConnection || needsPoll.getAndSet(false))) {
                                List<ImapResponse> untaggedResponses = new ArrayList<ImapResponse>(storedUntaggedResponses);
                                storedUntaggedResponses.clear();
                                processUntaggedResponses(untaggedResponses);
                                if (mMessageCount == -1) {
                                    throw new MessagingException("Message count = -1 for idling");
                                }
                                receiver.syncFolder(ImapFolderPusher.this);
                            }
                            if (stop.get()) {
                                continue;
                            }
                            long startUid = oldUidNext;

                            long newUidNext = uidNext;

                            if (newUidNext == -1) {
                                if (K9MailLib.isDebug()) {
                                    Log.d(LOG_TAG, "uidNext is -1, using search to find highest UID");
                                }
                                long highestUid = getHighestUid();
                                if (highestUid != -1L) {
                                    if (K9MailLib.isDebug())
                                        Log.d(LOG_TAG, "highest UID = " + highestUid);
                                    newUidNext = highestUid + 1;
                                    if (K9MailLib.isDebug())
                                        Log.d(LOG_TAG, "highest UID = " + highestUid
                                              + ", set newUidNext to " + newUidNext);
                                }
                            }

                            if (startUid < newUidNext - mStoreConfig.getDisplayCount()) {
                                startUid = newUidNext - mStoreConfig.getDisplayCount();
                            }
                            if (startUid < 1) {
                                startUid = 1;
                            }

                            lastUidNext = newUidNext;
                            if (newUidNext > startUid) {

                                if (K9MailLib.isDebug())
                                    Log.i(LOG_TAG, "Needs sync from uid " + startUid  + " to " + newUidNext + " for " + getLogId());
                                List<Message> messages = new ArrayList<Message>();
                                for (long uid = startUid; uid < newUidNext; uid++) {
                                    ImapMessage message = new ImapMessage("" + uid, ImapFolderPusher.this);
                                    messages.add(message);
                                }
                                if (!messages.isEmpty()) {
                                    pushMessages(messages, true);
                                }

                            } else {
                                List<ImapResponse> untaggedResponses;
                                while (!storedUntaggedResponses.isEmpty()) {
                                    if (K9MailLib.isDebug())
                                        Log.i(LOG_TAG, "Processing " + storedUntaggedResponses.size() + " untagged responses from previous commands for " + getLogId());
                                    untaggedResponses = new ArrayList<ImapResponse>(storedUntaggedResponses);
                                    storedUntaggedResponses.clear();
                                    processUntaggedResponses(untaggedResponses);
                                }

                                if (K9MailLib.isDebug())
                                    Log.i(LOG_TAG, "About to IDLE for " + getLogId());

                                receiver.setPushActive(getName(), true);
                                idling.set(true);
                                doneSent.set(false);

                                conn.setReadTimeout((mStoreConfig.getIdleRefreshMinutes() * 60 * 1000) + IDLE_READ_TIMEOUT_INCREMENT);
                                executeSimpleCommand(ImapCommands.COMMAND_IDLE, false, ImapFolderPusher.this);
                                idling.set(false);
                                delayTime.set(NORMAL_DELAY_TIME);
                                idleFailureCount.set(0);
                            }
                        } catch (Exception e) {
                            wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
                            storedUntaggedResponses.clear();
                            idling.set(false);
                            receiver.setPushActive(getName(), false);
                            try {
                                close();
                            } catch (Exception me) {
                                Log.e(LOG_TAG, "Got exception while closing for exception for " + getLogId(), me);
                            }
                            if (stop.get()) {
                                Log.i(LOG_TAG, "Got exception while idling, but stop is set for " + getLogId());
                            } else {
                                receiver.pushError("Push error for " + getName(), e);
                                Log.e(LOG_TAG, "Got exception while idling for " + getLogId(), e);
                                int delayTimeInt = delayTime.get();
                                receiver.sleep(wakeLock, delayTimeInt);
                                delayTimeInt *= 2;
                                if (delayTimeInt > MAX_DELAY_TIME) {
                                    delayTimeInt = MAX_DELAY_TIME;
                                }
                                delayTime.set(delayTimeInt);
                                if (idleFailureCount.incrementAndGet() > IDLE_FAILURE_COUNT_LIMIT) {
                                    Log.e(LOG_TAG, "Disabling pusher for " + getLogId() + " after " + idleFailureCount.get() + " consecutive errors");
                                    receiver.pushError("Push disabled for " + getName() + " after " + idleFailureCount.get() + " consecutive errors", e);
                                    stop.set(true);
                                }

                            }
                        }
                    }
                    receiver.setPushActive(getName(), false);
                    try {
                        if (K9MailLib.isDebug())
                            Log.i(LOG_TAG, "Pusher for " + getLogId() + " is exiting");
                        close();
                    } catch (Exception me) {
                        Log.e(LOG_TAG, "Got exception while closing for " + getLogId(), me);
                    } finally {
                        wakeLock.release();
                    }
                }
            };
            listeningThread = new Thread(runner);
            listeningThread.start();
        }

        @Override
        protected void handleUntaggedResponse(ImapResponse response) {
            if (response.getTag() == null && response.size() > 1) {
                Object responseType = response.get(1);
                if (ImapResponseParser.equalsIgnoreCase(responseType, "FETCH")
                        || ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE")
                        || ImapResponseParser.equalsIgnoreCase(responseType, "EXISTS")) {
                    if (K9MailLib.isDebug())
                        Log.d(LOG_TAG, "Storing response " + response + " for later processing");

                    storedUntaggedResponses.add(response);
                }
                handlePossibleUidNext(response);
            }
        }

        protected void processUntaggedResponses(List<ImapResponse> responses) throws MessagingException {
            boolean skipSync = false;
            int oldMessageCount = mMessageCount;
            if (oldMessageCount == -1) {
                skipSync = true;
            }
            List<Long> flagSyncMsgSeqs = new ArrayList<Long>();
            List<String> removeMsgUids = new LinkedList<String>();

            for (ImapResponse response : responses) {
                oldMessageCount += processUntaggedResponse(oldMessageCount, response, flagSyncMsgSeqs, removeMsgUids);
            }
            if (!skipSync) {
                if (oldMessageCount < 0) {
                    oldMessageCount = 0;
                }
                if (mMessageCount > oldMessageCount) {
                    syncMessages(mMessageCount, true);
                }
            }
            if (K9MailLib.isDebug())
                Log.d(LOG_TAG, "UIDs for messages needing flag sync are " + flagSyncMsgSeqs + "  for " + getLogId());

            if (!flagSyncMsgSeqs.isEmpty()) {
                syncMessages(flagSyncMsgSeqs);
            }
            if (!removeMsgUids.isEmpty()) {
                removeMessages(removeMsgUids);
            }
        }

        private void syncMessages(int end, boolean newArrivals) throws MessagingException {
            long oldUidNext = -1L;
            try {
                String pushStateS = receiver.getPushState(getName());
                ImapPushState pushState = ImapPushState.parse(pushStateS);
                oldUidNext = pushState.uidNext;
                if (K9MailLib.isDebug())
                    Log.i(LOG_TAG, "Got oldUidNext " + oldUidNext + " for " + getLogId());
            } catch (Exception e) {
                Log.e(LOG_TAG, "Unable to get oldUidNext for " + getLogId(), e);
            }

            List<? extends Message> messageList = getMessages(end, end, null, true, null);
            if (messageList != null && messageList.size() > 0) {
                long newUid = Long.parseLong(messageList.get(0).getUid());
                if (K9MailLib.isDebug())
                    Log.i(LOG_TAG, "Got newUid " + newUid + " for message " + end + " on " + getLogId());
                long startUid = oldUidNext;
                if (startUid < newUid - 10) {
                    startUid = newUid - 10;
                }
                if (startUid < 1) {
                    startUid = 1;
                }
                if (newUid >= startUid) {

                    if (K9MailLib.isDebug())
                        Log.i(LOG_TAG, "Needs sync from uid " + startUid  + " to " + newUid + " for " + getLogId());
                    List<Message> messages = new ArrayList<Message>();
                    for (long uid = startUid; uid <= newUid; uid++) {
                        ImapMessage message = new ImapMessage(Long.toString(uid), ImapFolderPusher.this);
                        messages.add(message);
                    }
                    if (!messages.isEmpty()) {
                        pushMessages(messages, true);
                    }
                }
            }
        }

        private void syncMessages(List<Long> flagSyncMsgSeqs) {
            try {
                List<? extends Message> messageList = getMessages(flagSyncMsgSeqs, true, null);

                List<Message> messages = new ArrayList<Message>();
                messages.addAll(messageList);
                pushMessages(messages, false);

            } catch (Exception e) {
                receiver.pushError("Exception while processing Push untagged responses", e);
            }
        }

        private void removeMessages(List<String> removeUids) {
            List<Message> messages = new ArrayList<Message>(removeUids.size());

            try {
                List<? extends Message> existingMessages = getMessagesFromUids(removeUids, true, null);
                for (Message existingMessage : existingMessages) {
                    needsPoll.set(true);
                    msgSeqUidMap.clear();
                    String existingUid = existingMessage.getUid();
                    Log.w(LOG_TAG, "Message with UID " + existingUid + " still exists on server, not expunging");
                    removeUids.remove(existingUid);
                }
                for (String uid : removeUids) {
                    ImapMessage message = new ImapMessage(uid, this);
                    try {
                        message.setFlagInternal(Flag.DELETED, true);
                    } catch (MessagingException me) {
                        Log.e(LOG_TAG, "Unable to set DELETED flag on message " + message.getUid());
                    }
                    messages.add(message);
                }
                receiver.messagesRemoved(this, messages);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Cannot remove EXPUNGEd messages", e);
            }

        }

        protected int processUntaggedResponse(long oldMessageCount, ImapResponse response, List<Long> flagSyncMsgSeqs, List<String> removeMsgUids) {
            super.handleUntaggedResponse(response);
            int messageCountDelta = 0;
            if (response.getTag() == null && response.size() > 1) {
                try {
                    Object responseType = response.get(1);
                    if (ImapResponseParser.equalsIgnoreCase(responseType, "FETCH")) {
                        Log.i(LOG_TAG, "Got FETCH " + response);
                        long msgSeq = response.getLong(0);

                        if (K9MailLib.isDebug())
                            Log.d(LOG_TAG, "Got untagged FETCH for msgseq " + msgSeq + " for " + getLogId());

                        if (!flagSyncMsgSeqs.contains(msgSeq)) {
                            flagSyncMsgSeqs.add(msgSeq);
                        }
                    }
                    if (ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE")) {
                        long msgSeq = response.getLong(0);
                        if (msgSeq <= oldMessageCount) {
                            messageCountDelta = -1;
                        }
                        if (K9MailLib.isDebug())
                            Log.d(LOG_TAG, "Got untagged EXPUNGE for msgseq " + msgSeq + " for " + getLogId());

                        List<Long> newSeqs = new ArrayList<Long>();
                        Iterator<Long> flagIter = flagSyncMsgSeqs.iterator();
                        while (flagIter.hasNext()) {
                            long flagMsg = flagIter.next();
                            if (flagMsg >= msgSeq) {
                                flagIter.remove();
                                if (flagMsg > msgSeq) {
                                    newSeqs.add(flagMsg--);
                                }
                            }
                        }
                        flagSyncMsgSeqs.addAll(newSeqs);


                        List<Long> msgSeqs = new ArrayList<Long>(msgSeqUidMap.keySet());
                        Collections.sort(msgSeqs);  // Have to do comparisons in order because of msgSeq reductions

                        for (long msgSeqNum : msgSeqs) {
                            if (K9MailLib.isDebug()) {
                                Log.v(LOG_TAG, "Comparing EXPUNGEd msgSeq " + msgSeq + " to " + msgSeqNum);
                            }
                            if (msgSeqNum == msgSeq) {
                                String uid = msgSeqUidMap.get(msgSeqNum);
                                if (K9MailLib.isDebug()) {
                                    Log.d(LOG_TAG, "Scheduling removal of UID " + uid + " because msgSeq " + msgSeqNum + " was expunged");
                                }
                                removeMsgUids.add(uid);
                                msgSeqUidMap.remove(msgSeqNum);
                            } else if (msgSeqNum > msgSeq) {
                                String uid = msgSeqUidMap.get(msgSeqNum);
                                if (K9MailLib.isDebug()) {
                                    Log.d(LOG_TAG, "Reducing msgSeq for UID " + uid + " from " + msgSeqNum + " to " + (msgSeqNum - 1));
                                }
                                msgSeqUidMap.remove(msgSeqNum);
                                msgSeqUidMap.put(msgSeqNum - 1, uid);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Could not handle untagged FETCH for " + getLogId(), e);
                }
            }
            return messageCountDelta;
        }


        private void pushMessages(List<Message> messages, boolean newArrivals) {
            RuntimeException holdException = null;
            try {
                if (newArrivals) {
                    receiver.messagesArrived(this, messages);
                } else {
                    receiver.messagesFlagsChanged(this, messages);
                }
            } catch (RuntimeException e) {
                holdException = e;
            }

            if (holdException != null) {
                throw holdException;
            }
        }

        public void stop() {
            stop.set(true);
            if (listeningThread != null) {
                listeningThread.interrupt();
            }
            ImapConnection conn = mConnection;
            if (conn != null) {
                if (K9MailLib.isDebug())
                    Log.v(LOG_TAG, "Closing mConnection to stop pushing for " + getLogId());
                conn.close();
            } else {
                Log.w(LOG_TAG, "Attempt to interrupt null mConnection to stop pushing on folderPusher for " + getLogId());
            }
        }

        @Override
        public void handleAsyncUntaggedResponse(ImapResponse response) {
            if (K9MailLib.isDebug())
                Log.v(LOG_TAG, "Got async response: " + response);

            if (stop.get()) {
                if (K9MailLib.isDebug())
                    Log.d(LOG_TAG, "Got async untagged response: " + response + ", but stop is set for " + getLogId());

                try {
                    sendDone();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
                }
            } else {
                if (response.getTag() == null) {
                    if (response.size() > 1) {
                        boolean started = false;
                        Object responseType = response.get(1);
                        if (ImapResponseParser.equalsIgnoreCase(responseType, "EXISTS") || ImapResponseParser.equalsIgnoreCase(responseType, "EXPUNGE") ||
                                ImapResponseParser.equalsIgnoreCase(responseType, "FETCH")) {
                            if (!started) {
                                wakeLock.acquire(PUSH_WAKE_LOCK_TIMEOUT);
                                started = true;
                            }

                            if (K9MailLib.isDebug())
                                Log.d(LOG_TAG, "Got useful async untagged response: " + response + " for " + getLogId());

                            try {
                                sendDone();
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Exception while sending DONE for " + getLogId(), e);
                            }
                        }
                    } else if (response.isContinuationRequested()) {
                        if (K9MailLib.isDebug())
                            Log.d(LOG_TAG, "Idling " + getLogId());

                        wakeLock.release();
                    }
                }
            }
        }
    }
    @Override
    public Pusher getPusher(PushReceiver receiver) {
        return new ImapPusher(this, receiver);
    }

    public class ImapPusher implements Pusher {
        private final ImapStore mStore;
        final PushReceiver mReceiver;
        private long lastRefresh = -1;

        final Map<String, ImapFolderPusher> folderPushers = new HashMap<String, ImapFolderPusher>();

        public ImapPusher(ImapStore store, PushReceiver receiver) {
            mStore = store;
            mReceiver = receiver;
        }

        @Override
        public void start(List<String> folderNames) {
            stop();
            synchronized (folderPushers) {
                setLastRefresh(System.currentTimeMillis());
                for (String folderName : folderNames) {
                    ImapFolderPusher pusher = folderPushers.get(folderName);
                    if (pusher == null) {
                        pusher = new ImapFolderPusher(mStore, folderName, mReceiver);
                        folderPushers.put(folderName, pusher);
                        pusher.start();
                    }
                }
            }
        }

        @Override
        public void refresh() {
            synchronized (folderPushers) {
                for (ImapFolderPusher folderPusher : folderPushers.values()) {
                    try {
                        folderPusher.refresh();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Got exception while refreshing for " + folderPusher.getName(), e);
                    }
                }
            }
        }

        @Override
        public void stop() {
            if (K9MailLib.isDebug())
                Log.i(LOG_TAG, "Requested stop of IMAP pusher");

            synchronized (folderPushers) {
                for (ImapFolderPusher folderPusher : folderPushers.values()) {
                    try {
                        if (K9MailLib.isDebug())
                            Log.i(LOG_TAG, "Requesting stop of IMAP folderPusher " + folderPusher.getName());
                        folderPusher.stop();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Got exception while stopping " + folderPusher.getName(), e);
                    }
                }
                folderPushers.clear();
            }
        }

        @Override
        public int getRefreshInterval() {
            return (mStoreConfig.getIdleRefreshMinutes() * 60 * 1000);
        }

        @Override
        public long getLastRefresh() {
            return lastRefresh;
        }

        @Override
        public void setLastRefresh(long lastRefresh) {
            this.lastRefresh = lastRefresh;
        }
    }

    protected static class ImapPushState {
        protected long uidNext;
        protected ImapPushState(long nUidNext) {
            uidNext = nUidNext;
        }
        protected static ImapPushState parse(String pushState) {
            long newUidNext = -1L;
            if (pushState != null) {
                StringTokenizer tokenizer = new StringTokenizer(pushState, ";");
                while (tokenizer.hasMoreTokens()) {
                    StringTokenizer thisState = new StringTokenizer(tokenizer.nextToken(), "=");
                    if (thisState.hasMoreTokens()) {
                        String key = thisState.nextToken();

                        if ("uidNext".equalsIgnoreCase(key) && thisState.hasMoreTokens()) {
                            String value = thisState.nextToken();
                            try {
                                newUidNext = Long.parseLong(value);
                            } catch (NumberFormatException e) {
                                Log.e(LOG_TAG, "Unable to part uidNext value " + value, e);
                            }

                        }
                    }
                }
            }
            return new ImapPushState(newUidNext);
        }
        @Override
        public String toString() {
            return "uidNext=" + uidNext;
        }

    }
    protected interface ImapSearcher {
        List<ImapResponse> search() throws IOException, MessagingException;
    }

    private static class FetchBodyCallback implements ImapResponseCallback {
        private Map<String, Message> mMessageMap;

        FetchBodyCallback(Map<String, Message> messageMap) {
            mMessageMap = messageMap;
        }

        @Override
        public Object foundLiteral(ImapResponse response,
                                   FixedLengthInputStream literal) throws MessagingException, IOException {
            if (response.getTag() == null &&
                    ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
                ImapList fetchList = (ImapList)response.getKeyedValue("FETCH");
                String uid = fetchList.getKeyedString("UID");

                ImapMessage message = (ImapMessage) mMessageMap.get(uid);
                message.parse(literal);

                // Return placeholder object
                return 1;
            }
            return null;
        }
    }

    private static class FetchPartCallback implements ImapResponseCallback {
        private Part mPart;

        FetchPartCallback(Part part) {
            mPart = part;
        }

        @Override
        public Object foundLiteral(ImapResponse response,
                                   FixedLengthInputStream literal) throws MessagingException, IOException {
            if (response.getTag() == null &&
                    ImapResponseParser.equalsIgnoreCase(response.get(1), "FETCH")) {
                //TODO: check for correct UID

                String contentTransferEncoding = mPart
                        .getHeader(MimeHeader.HEADER_CONTENT_TRANSFER_ENCODING)[0];
                String contentType = mPart
                        .getHeader(MimeHeader.HEADER_CONTENT_TYPE)[0];

                return MimeUtility.createBody(literal, contentTransferEncoding,
                        contentType);
            }
            return null;
        }
    }

    private static String combine(Object[] parts, char separator) {
        if (parts == null) {
            return null;
        }
        return TextUtils.join(String.valueOf(separator), parts);
    }

    private class StoreImapSettings implements ImapSettings {
        @Override
        public String getHost() {
            return mHost;
        }

        @Override
        public int getPort() {
            return mPort;
        }

        @Override
        public ConnectionSecurity getConnectionSecurity() {
            return mConnectionSecurity;
        }

        @Override
        public AuthType getAuthType() {
            return mAuthType;
        }

        @Override
        public String getUsername() {
            return mUsername;
        }

        @Override
        public String getPassword() {
            return mPassword;
        }

        @Override
        public String getClientCertificateAlias() {
            return mClientCertificateAlias;
        }

        @Override
        public boolean useCompression(final NetworkType type) {
            return mStoreConfig.useCompression(type);
        }

        @Override
        public String getPathPrefix() {
            return mPathPrefix;
        }

        @Override
        public void setPathPrefix(String prefix) {
            mPathPrefix = prefix;
        }

        @Override
        public String getPathDelimiter() {
            return mPathDelimiter;
        }

        @Override
        public void setPathDelimiter(String delimiter) {
            mPathDelimiter = delimiter;
        }

        @Override
        public String getCombinedPrefix() {
            return mCombinedPrefix;
        }

        @Override
        public void setCombinedPrefix(String prefix) {
            mCombinedPrefix = prefix;
        }
    }
}
