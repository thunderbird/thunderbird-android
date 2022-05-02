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

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * <pre>
 * TODO Need a default response handler for things like folder updates
 * </pre>
 */
class RealImapStore implements ImapStore, ImapConnectionManager, InternalImapStore {
    private final ImapStoreConfig config;
    private final TrustedSocketFactory trustedSocketFactory;
    private Set<Flag> permanentFlagsIndex = EnumSet.noneOf(Flag.class);
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
    private volatile int connectionGeneration = 1;


    public RealImapStore(ServerSettings serverSettings, ImapStoreConfig config,
            TrustedSocketFactory trustedSocketFactory, OAuth2TokenProvider oauthTokenProvider) {
        this.config = config;
        this.trustedSocketFactory = trustedSocketFactory;

        host = serverSettings.host;
        port = serverSettings.port;

        connectionSecurity = serverSettings.connectionSecurity;
        this.oauthTokenProvider = oauthTokenProvider;

        authType = serverSettings.authenticationType;
        username = serverSettings.username;
        password = serverSettings.password;
        clientCertificateAlias = serverSettings.clientCertificateAlias;

        boolean autoDetectNamespace = ImapStoreSettings.getAutoDetectNamespace(serverSettings);
        String pathPrefixSetting = ImapStoreSettings.getPathPrefix(serverSettings);

        // Make extra sure pathPrefix is null if "auto-detect namespace" is configured
        pathPrefix = autoDetectNamespace ? null : pathPrefixSetting;

        folderNameCodec = FolderNameCodec.newInstance();
    }

    public ImapFolder getFolder(String name) {
        return new RealImapFolder(this, this, name, folderNameCodec);
    }

    @Override
    @NotNull
    public String getCombinedPrefix() {
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

            if (RealImapFolder.INBOX.equalsIgnoreCase(serverId)) {
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
        folders.add(new FolderListItem(RealImapFolder.INBOX, RealImapFolder.INBOX, FolderType.INBOX, RealImapFolder.INBOX));
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

    @Override
    @NotNull
    public ImapConnection getConnection() throws MessagingException {
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

    @Override
    public void releaseConnection(ImapConnection connection) {
        if (connection != null && connection.isConnected()) {
            if (connection.getConnectionGeneration() == connectionGeneration) {
                synchronized (connections) {
                    connections.offer(connection);
                }
            } else {
                connection.close();
            }
        }
    }

    @Override
    public void closeAllConnections() {
        Timber.v("ImapStore.closeAllConnections()");

        List<ImapConnection> connectionsToClose;
        synchronized (connections) {
            connectionGeneration++;
            connectionsToClose = new ArrayList<>(connections);
            connections.clear();
        }

        for (ImapConnection connection : connectionsToClose) {
            connection.close();
        }
    }

    ImapConnection createImapConnection() {
        return new RealImapConnection(
                new StoreImapSettings(),
                trustedSocketFactory,
                oauthTokenProvider,
                connectionGeneration);
    }

    @Override
    @NotNull
    public String getLogLabel() {
        return config.getLogLabel();
    }

    @Override
    @NotNull
    public Set<Flag> getPermanentFlagsIndex() {
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
        public boolean useCompression() {
            return config.useCompression();
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
