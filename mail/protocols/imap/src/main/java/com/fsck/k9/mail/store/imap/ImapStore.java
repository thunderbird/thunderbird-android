package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.ConnectivityManager;
import androidx.annotation.Nullable;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import timber.log.Timber;


/**
 * <pre>
 * TODO Need a default response handler for things like folder updates
 * </pre>
 */
public class ImapStore {
    private final ImapStoreConfig config;
    private final TrustedSocketFactory trustedSocketFactory;
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
    private final Deque<ImapConnection> connections = new LinkedList<>();
    private FolderNameCodec folderNameCodec;

    /**
     * Cache of ImapFolder objects. ImapFolders are attached to a given folder on the server
     * and as long as their associated connection remains open they are reusable between
     * requests. This cache lets us make sure we always reuse, if possible, for a given
     * folder name.
     */
    private final Map<String, ImapFolder> folderCache = new HashMap<>();


    public ImapStore(ImapStoreSettings serverSettings, ImapStoreConfig config,
            TrustedSocketFactory trustedSocketFactory, ConnectivityManager connectivityManager,
            OAuth2TokenProvider oauthTokenProvider) {
        this.config = config;
        this.trustedSocketFactory = trustedSocketFactory;

        host = serverSettings.host;
        port = serverSettings.port;

        connectionSecurity = serverSettings.connectionSecurity;
        this.connectivityManager = connectivityManager;
        this.oauthTokenProvider = oauthTokenProvider;

        authType = serverSettings.authenticationType;
        username = serverSettings.username;
        password = serverSettings.password;
        clientCertificateAlias = serverSettings.clientCertificateAlias;

        // Make extra sure pathPrefix is null if "auto-detect namespace" is configured
        pathPrefix = (serverSettings.autoDetectNamespace) ? null : serverSettings.pathPrefix;

        folderNameCodec = FolderNameCodec.newInstance();
    }

    public ImapFolder getFolder(String name) {
        ImapFolder folder;
        synchronized (folderCache) {
            folder = folderCache.get(name);
            if (folder == null) {
                folder = new ImapFolder(this, name);
                folderCache.put(name, folder);
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

    public List<FolderListItem> getFolders() throws MessagingException {
        ImapConnection connection = getConnection();

        try {
            List<FolderListItem> folders = listFolders(connection, false);

            if (!config.isSubscribedFoldersOnly()) {
                return folders;
            }

            List<FolderListItem> subscribedFolders = listFolders(connection, true);
            return limitToSubscribedFolders(folders, subscribedFolders);
        } catch (IOException | MessagingException ioe) {
            connection.close();
            throw new MessagingException("Unable to get folder list.", ioe);
        } finally {
            releaseConnection(connection);
        }
    }

    private List<FolderListItem> limitToSubscribedFolders(List<FolderListItem> folders,
            List<FolderListItem> subscribedFolders) {
        Set<String> subscribedFolderNames = new HashSet<>(subscribedFolders.size());
        for (FolderListItem subscribedFolder : subscribedFolders) {
            subscribedFolderNames.add(subscribedFolder.getServerId());
        }

        List<FolderListItem> filteredFolders = new ArrayList<>();
        for (FolderListItem folder : folders) {
            if (subscribedFolderNames.contains(folder.getServerId())) {
                filteredFolders.add(folder);
            }
        }

        return filteredFolders;
    }

    private List<FolderListItem> listFolders(ImapConnection connection, boolean subscribedOnly) throws IOException,
            MessagingException {

        String commandFormat;
        if (subscribedOnly) {
            commandFormat = "LSUB \"\" %s";
        } else if (connection.hasCapability(Capabilities.SPECIAL_USE) &&
                connection.hasCapability(Capabilities.LIST_EXTENDED)) {
            commandFormat = "LIST \"\" %s RETURN (SPECIAL-USE)";
        } else {
            commandFormat = "LIST \"\" %s";
        }

        String encodedListPrefix = ImapUtility.encodeString(getCombinedPrefix() + "*");
        List<ImapResponse> responses = connection.executeSimpleCommand(String.format(commandFormat, encodedListPrefix));

        List<ListResponse> listResponses = (subscribedOnly) ?
                ListResponse.parseLsub(responses) :
                ListResponse.parseList(responses);

        Map<String, FolderListItem> folderMap = new HashMap<>(listResponses.size());
        for (ListResponse listResponse : listResponses) {
            String serverId = listResponse.getName();

            if (pathDelimiter == null) {
                pathDelimiter = listResponse.getHierarchyDelimiter();
                combinedPrefix = null;
            }

            if (ImapFolder.INBOX.equalsIgnoreCase(serverId)) {
                continue;
            } else if (listResponse.hasAttribute("\\NoSelect")) {
                continue;
            }

            String name = getFolderDisplayName(serverId);
            String oldServerId = getOldServerId(serverId);

            FolderType type;
            if (listResponse.hasAttribute("\\Archive") || listResponse.hasAttribute("\\All")) {
                type = FolderType.ARCHIVE;
            } else if (listResponse.hasAttribute("\\Drafts")) {
                type = FolderType.DRAFTS;
            } else if (listResponse.hasAttribute("\\Sent")) {
                type = FolderType.SENT;
            } else if (listResponse.hasAttribute("\\Junk")) {
                type = FolderType.SPAM;
            } else if (listResponse.hasAttribute("\\Trash")) {
                type = FolderType.TRASH;
            } else {
                type = FolderType.REGULAR;
            }

            FolderListItem existingItem = folderMap.get(serverId);
            if (existingItem == null || existingItem.getType() == FolderType.REGULAR) {
                folderMap.put(serverId, new FolderListItem(serverId, name, type, oldServerId));
            }
        }

        List<FolderListItem> folders = new ArrayList<>(folderMap.size() + 1);
        folders.add(new FolderListItem(ImapFolder.INBOX, ImapFolder.INBOX, FolderType.INBOX, ImapFolder.INBOX));
        folders.addAll(folderMap.values());

        return folders;
    }

    private String getFolderDisplayName(String serverId) {
        String decodedFolderName;
        try {
            decodedFolderName = folderNameCodec.decode(serverId);
        } catch (CharacterCodingException e) {
            Timber.w(e, "Folder name not correctly encoded with the UTF-7 variant as defined by RFC 3501: %s",
                    serverId);

            decodedFolderName = serverId;
        }

        String folderNameWithoutPrefix = removePrefixFromFolderName(decodedFolderName);
        return folderNameWithoutPrefix != null ? folderNameWithoutPrefix : decodedFolderName;
    }

    @Nullable
    private String getOldServerId(String serverId) {
        String decodedFolderName;
        try {
            decodedFolderName = folderNameCodec.decode(serverId);
        } catch (CharacterCodingException e) {
            // Previous versions of K-9 Mail ignored folders with invalid UTF-7 encoding
            return null;
        }

        return removePrefixFromFolderName(decodedFolderName);
    }

    @Nullable
    private String removePrefixFromFolderName(String folderName) {
        String prefix = getCombinedPrefix();
        int prefixLength = prefix.length();
        if (prefixLength == 0) {
            return folderName;
        }

        if (!folderName.startsWith(prefix)) {
            // Folder name doesn't start with our configured prefix. But right now when building commands we prefix all
            // folders except the INBOX with the prefix. So we won't be able to use this folder.
            return null;
        }

        return folderName.substring(prefixLength);
    }

    public void checkSettings() throws MessagingException {
        try {
            ImapConnection connection = createImapConnection();

            connection.open();
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
                trustedSocketFactory,
                connectivityManager,
                oauthTokenProvider);
    }

    FolderNameCodec getFolderNameCodec() {
        return folderNameCodec;
    }

    String getLogLabel() {
        return config.getLogLabel();
    }

    Set<Flag> getPermanentFlagsIndex() {
        return permanentFlagsIndex;
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
            return config.useCompression(type);
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
        public void setCombinedPrefix(String prefix) {
            combinedPrefix = prefix;
        }
    }
}
