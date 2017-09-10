package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;
import timber.log.Timber;


/**
 * <pre>
 * TODO Need to start keeping track of UIDVALIDITY
 * TODO Need a default response handler for things like folder updates
 * </pre>
 */
public class ImapStore extends RemoteStore {
    private Set<Flag> permanentFlagsIndex = EnumSet.noneOf(Flag.class);
    private ConnectivityManager connectivityManager;
    private OAuth2TokenProvider oauthTokenProvider;

    private String host;
    private int port;
    private String username;
    private String password;
    private String clientCertificateAlias;
    private ConnectionSecurity connectionSecurity;
    private AuthType authType;
    private String pathPrefix;
    private String combinedPrefix = null;
    private String pathDelimiter = null;
    private final Deque<ImapConnection> connections = new LinkedList<ImapConnection>();
    private FolderNameCodec folderNameCodec;

    /**
     * Cache of ImapFolder objects. ImapFolders are attached to a given folder on the server
     * and as long as their associated connection remains open they are reusable between
     * requests. This cache lets us make sure we always reuse, if possible, for a given
     * folder name.
     */
    private final Map<String, ImapFolder> folderCache = new HashMap<String, ImapFolder>();


    public static ImapStoreSettings decodeUri(String uri) {
        return ImapStoreUriDecoder.decode(uri);
    }

    public static String createUri(ServerSettings server) {
        return ImapStoreUriCreator.create(server);
    }

    public ImapStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory,
            ConnectivityManager connectivityManager, OAuth2TokenProvider oauthTokenProvider) throws MessagingException {
        super(storeConfig, trustedSocketFactory);

        ImapStoreSettings settings;
        try {
            settings = decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        host = settings.host;
        port = settings.port;

        connectionSecurity = settings.connectionSecurity;
        this.connectivityManager = connectivityManager;
        this.oauthTokenProvider = oauthTokenProvider;

        authType = settings.authenticationType;
        username = settings.username;
        password = settings.password;
        clientCertificateAlias = settings.clientCertificateAlias;

        // Make extra sure pathPrefix is null if "auto-detect namespace" is configured
        pathPrefix = (settings.autoDetectNamespace) ? null : settings.pathPrefix;

        folderNameCodec = FolderNameCodec.newInstance();
    }

    @Override
    @NonNull public ImapFolder getFolder(String folderId) {
        ImapFolder folder;
        synchronized (folderCache) {
            folder = folderCache.get(folderId);
            if (folder == null) {
                folder = new ImapFolder(this, folderId);
                folderCache.put(folderId, folder);
            }
        }

        return folder;
    }

    String getCombinedPrefix() {
        if (combinedPrefix == null) {
            if (pathPrefix != null) {
                String tmpPrefix = pathPrefix.trim();
                String tmpDelim = (pathDelimiter != null ? pathDelimiter.trim() : "");
                if (tmpPrefix.endsWith(tmpDelim)) {
                    combinedPrefix = tmpPrefix;
                } else if (tmpPrefix.length() > 0) {
                    combinedPrefix = tmpPrefix + tmpDelim;
                } else {
                    combinedPrefix = "";
                }
            } else {
                combinedPrefix = "";
            }
        }

        return combinedPrefix;
    }

    @Override
    @NonNull
    public List<ImapFolder> getFolders(boolean forceListAll) throws MessagingException {
        return getPersonalNamespaces(forceListAll);
    }

    @Override
    @NonNull
    public List<ImapFolder> getSubFolders(final String parentFolderId, boolean forceListAll) throws MessagingException {
        List<ImapFolder> folders = getPersonalNamespaces(forceListAll);
        List<ImapFolder> subFolders = new ArrayList<>();

        for (ImapFolder folder: folders) {
            if (folder.getId().startsWith(parentFolderId) && folder.getId().length() != parentFolderId.length()) {
                subFolders.add(folder);
            }
        }

        return subFolders;
    }

    @NonNull
    public List<ImapFolder> getPersonalNamespaces(boolean forceListAll) throws MessagingException {
        ImapConnection connection = getConnection();

        try {
            Set<String> folderIds = listFolders(connection, false);

            if (forceListAll || !mStoreConfig.subscribedFoldersOnly()) {
                return getFolders(folderIds);
            }

            Set<String> subscribedFolders = listFolders(connection, true);

            folderIds.retainAll(subscribedFolders);

            return getFolders(folderIds);
        } catch (IOException | MessagingException ioe) {
            connection.close();
            throw new MessagingException("Unable to get folder list.", ioe);
        } finally {
            releaseConnection(connection);
        }
    }

    private Set<String> listFolders(ImapConnection connection, boolean subscribedOnly) throws IOException,
            MessagingException {
        String commandResponse = subscribedOnly ? "LSUB" : "LIST";

        String command = String.format("%s \"\" %s", commandResponse,
                ImapUtility.encodeString(getCombinedPrefix() + "*"));

        List<ImapResponse> responses =
                connection.executeSimpleCommand(command);

        List<ListResponse> listResponses = (subscribedOnly) ?
                ListResponse.parseLsub(responses) : ListResponse.parseList(responses);

        Set<String> folderIds = new HashSet<>(listResponses.size());

        for (ListResponse listResponse : listResponses) {
            boolean includeFolder = true;

            String decodedFolderId;
            try {
                decodedFolderId = folderNameCodec.decode(listResponse.getName());
            } catch (CharacterCodingException e) {
                Timber.w(e,
                        "Folder name not correctly encoded with the UTF-7 variant  as defined by RFC 3501: %s",
                        listResponse.getName());

                //TODO: Use the raw name returned by the server for all commands that require
                //      a folder name. Use the decoded name only for showing it to the user.

                // We currently just skip folders with malformed names.
                continue;
            }

            String folder = decodedFolderId;

            if (pathDelimiter == null) {
                pathDelimiter = listResponse.getHierarchyDelimiter();
                combinedPrefix = null;
            }

            if (folder.equalsIgnoreCase(mStoreConfig.getInboxFolderId())) {
                continue;
            } else if (folder.equals(mStoreConfig.getOutboxFolderId())) {
                /*
                 * There is a folder on the server with the same name as our local
                 * outbox. Until we have a good plan to deal with this situation
                 * we simply ignore the folder on the server.
                 */
                continue;
            } else {
                String combinedPrefix = getCombinedPrefix();
                int prefixLength = combinedPrefix.length();
                if (prefixLength > 0) {
                    // Strip prefix from the folder name
                    if (folder.length() >= prefixLength) {
                        folder = folder.substring(prefixLength);
                    }
                    if (!decodedFolderId.equalsIgnoreCase(combinedPrefix + folder)) {
                        includeFolder = false;
                    }
                }
            }

            if (listResponse.hasAttribute("\\NoSelect")) {
                includeFolder = false;
            }

            if (includeFolder) {
                folderIds.add(folder);
            }
        }

        folderIds.add(mStoreConfig.getInboxFolderId());

        return folderIds;
    }

    void autoconfigureFolders(final ImapConnection connection) throws IOException, MessagingException {
        if (!connection.hasCapability(Capabilities.SPECIAL_USE)) {
            if (K9MailLib.isDebug()) {
                Timber.d("No detected folder auto-configuration methods.");
            }
            return;
        }

        if (K9MailLib.isDebug()) {
            Timber.d("Folder auto-configuration: Using RFC6154/SPECIAL-USE.");
        }

        String command = String.format("LIST (SPECIAL-USE) \"\" %s", ImapUtility.encodeString(getCombinedPrefix() + "*"));
        List<ImapResponse> responses = connection.executeSimpleCommand(command);

        List<ListResponse> listResponses = ListResponse.parseList(responses);

        for (ListResponse listResponse : listResponses) {
            String decodedFolderName;
            try {
                decodedFolderName = folderNameCodec.decode(listResponse.getName());
            } catch (CharacterCodingException e) {
                Timber.w(e, "Folder name not correctly encoded with the UTF-7 variant as defined by RFC 3501: %s",
                        listResponse.getName());
                // We currently just skip folders with malformed names.
                continue;
            }

            if (pathDelimiter == null) {
                pathDelimiter = listResponse.getHierarchyDelimiter();
                combinedPrefix = null;
            }

            if (listResponse.hasAttribute("\\Archive") || listResponse.hasAttribute("\\All")) {
                mStoreConfig.setArchiveFolderId(decodedFolderName);
                if (K9MailLib.isDebug()) {
                    Timber.d("Folder auto-configuration detected Archive folder: %s", decodedFolderName);
                }
            } else if (listResponse.hasAttribute("\\Drafts")) {
                mStoreConfig.setDraftsFolderId(decodedFolderName);
                if (K9MailLib.isDebug()) {
                    Timber.d("Folder auto-configuration detected Drafts folder: %s", decodedFolderName);
                }
            } else if (listResponse.hasAttribute("\\Sent")) {
                mStoreConfig.setSentFolderId(decodedFolderName);
                if (K9MailLib.isDebug()) {
                    Timber.d("Folder auto-configuration detected Sent folder: %s", decodedFolderName);
                }
            } else if (listResponse.hasAttribute("\\Junk")) {
                mStoreConfig.setSpamFolderId(decodedFolderName);
                if (K9MailLib.isDebug()) {
                    Timber.d("Folder auto-configuration detected Spam folder: %s", decodedFolderName);
                }
            } else if (listResponse.hasAttribute("\\Trash")) {
                mStoreConfig.setTrashFolderId(decodedFolderName);
                if (K9MailLib.isDebug()) {
                    Timber.d("Folder auto-configuration detected Trash folder: %s", decodedFolderName);
                }
            }
        }
    }

    @Override
    public void checkSettings() throws MessagingException {
        try {
            ImapConnection connection = createImapConnection();

            connection.open();
            autoconfigureFolders(connection);
            connection.close();
        } catch (IOException ioe) {
            throw new MessagingException("Unable to connect", ioe);
        }
    }

    ImapConnection getConnection() throws MessagingException {
        ImapConnection connection;
        while ((connection = pollConnection()) != null) {
            try {
                connection.executeSimpleCommand(Commands.NOOP);
                break;
            } catch (IOException ioe) {
                connection.close();
            }
        }

        if (connection == null) {
            connection = createImapConnection();
        }

        return connection;
    }

    private ImapConnection pollConnection() {
        synchronized (connections) {
            return connections.poll();
        }
    }

    void releaseConnection(ImapConnection connection) {
        if (connection != null && connection.isConnected()) {
            synchronized (connections) {
                connections.offer(connection);
            }
        }
    }

    ImapConnection createImapConnection() {
        return new ImapConnection(
                new StoreImapSettings(),
                mTrustedSocketFactory,
                connectivityManager,
                oauthTokenProvider);
    }

    FolderNameCodec getFolderNameCodec() {
        return folderNameCodec;
    }

    private List<ImapFolder> getFolders(Collection<String> folderIds) {
        List<ImapFolder> folders = new ArrayList<>(folderIds.size());

        for (String folderId : folderIds) {
            ImapFolder imapFolder = getFolder(folderId);
            folders.add(imapFolder);
        }

        return folders;
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
        return permanentFlagsIndex;
    }

    @Override
    public Pusher getPusher(PushReceiver receiver) {
        return new ImapPusher(this, receiver);
    }

    String getParentId(@NonNull String id) {
        if (pathDelimiter == null || id.lastIndexOf(pathDelimiter) == -1) {
            return "";
        } else {
            return id.substring(0, id.lastIndexOf(pathDelimiter));
        }
    }

    public String getFolderName(String id) {
        if (pathDelimiter == null || id.lastIndexOf(pathDelimiter) == -1) {
            return id;
        } else {
            return id.substring(id.lastIndexOf(pathDelimiter) + pathDelimiter.length());
        }
    }


    private class StoreImapSettings implements ImapSettings {
        @Override
        public String getHost() {
            return host;
        }

        @Override
        public int getPort() {
            return port;
        }

        @Override
        public ConnectionSecurity getConnectionSecurity() {
            return connectionSecurity;
        }

        @Override
        public AuthType getAuthType() {
            return authType;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getClientCertificateAlias() {
            return clientCertificateAlias;
        }

        @Override
        public boolean useCompression(final NetworkType type) {
            return mStoreConfig.useCompression(type);
        }

        @Override
        public String getPathPrefix() {
            return pathPrefix;
        }

        @Override
        public void setPathPrefix(String prefix) {
            pathPrefix = prefix;
        }

        @Override
        public String getPathDelimiter() {
            return pathDelimiter;
        }

        @Override
        public void setPathDelimiter(String delimiter) {
            pathDelimiter = delimiter;
        }

        @Override
        public String getCombinedPrefix() {
            return combinedPrefix;
        }

        @Override
        public void setCombinedPrefix(String prefix) {
            combinedPrefix = prefix;
        }
    }
}
