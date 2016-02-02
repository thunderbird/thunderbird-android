
package com.fsck.k9.mail.store.imap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.SimpleDateFormat;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.net.ConnectivityManager;
import android.util.Log;

import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;

import com.beetstra.jutf7.CharsetProvider;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


/**
 * <pre>
 * TODO Need to start keeping track of UIDVALIDITY
 * TODO Need a default response handler for things like folder updates
 * </pre>
 */
public class ImapStore extends RemoteStore {
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

    String getCombinedPrefix() {
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

        if (connection.hasCapability(Capabilities.XLIST)) {
            if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration: Using XLIST.");
            commandResponse = Responses.XLIST;
        } else if(connection.hasCapability(Capabilities.SPECIAL_USE)) {
            if (K9MailLib.isDebug()) Log.d(LOG_TAG, "Folder auto-configuration: Using RFC6154/SPECIAL-USE.");
            commandResponse = Responses.LIST;
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
                    if (attribute.equals("\\Archive") || attribute.equals("\\AllMail")) {
                        mStoreConfig.setArchiveFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Folder auto-configuration detected Archive folder: " + decodedFolderName);
                        }
                    } else if (attribute.equals("\\Drafts")) {
                        mStoreConfig.setDraftsFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Folder auto-configuration detected Drafts folder: " + decodedFolderName);
                        }
                    } else if (attribute.equals("\\Sent")) {
                        mStoreConfig.setSentFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Folder auto-configuration detected Sent folder: " + decodedFolderName);
                        }
                    } else if (attribute.equals("\\Spam") || attribute.equals("\\Junk")) {
                        //rfc6154 just mentions \Junk
                        mStoreConfig.setSpamFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Folder auto-configuration detected Spam folder: " + decodedFolderName);
                        }
                    } else if (attribute.equals("\\Trash")) {
                        mStoreConfig.setTrashFolderName(decodedFolderName);
                        if (K9MailLib.isDebug()) {
                            Log.d(LOG_TAG, "Folder auto-configuration detected Trash folder: " + decodedFolderName);
                        }
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

    ImapConnection getConnection() throws MessagingException {
        synchronized (mConnections) {
            ImapConnection connection;
            while ((connection = mConnections.poll()) != null) {
                try {
                    connection.executeSimpleCommand(Commands.NOOP);
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

    void releaseConnection(ImapConnection connection) {
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
    static String encodeString(String str) {
        return "\"" + str.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    String encodeFolderName(String name) {
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

    StoreConfig getStoreConfig() {
        return mStoreConfig;
    }

    Set<Flag> getPermanentFlagsIndex() {
        return mPermanentFlagsIndex;
    }

    @Override
    public Pusher getPusher(PushReceiver receiver) {
        return new ImapPusher(this, receiver);
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
