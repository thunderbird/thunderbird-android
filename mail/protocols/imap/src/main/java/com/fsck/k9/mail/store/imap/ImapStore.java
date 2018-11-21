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
import android.support.annotation.Nullable;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
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
    private final Deque<ImapConnection> connections = new LinkedList<>();
    private FolderNameCodec folderNameCodec;

    /**
     * Cache of ImapFolder objects. ImapFolders are attached to a given folder on the server
     * and as long as their associated connection remains open they are reusable between
     * requests. This cache lets us make sure we always reuse, if possible, for a given
     * folder name.
     */
    private final Map<String, ImapFolder> folderCache = new HashMap<>();


    public ImapStore(ImapStoreSettings serverSettings, StoreConfig storeConfig,
            TrustedSocketFactory trustedSocketFactory, ConnectivityManager connectivityManager,
            OAuth2TokenProvider oauthTokenProvider) {
        super(storeConfig, trustedSocketFactory);

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

    @Override
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

    @Override
    public List<ImapFolder> getPersonalNamespaces() throws MessagingException {
        ImapConnection connection = getConnection();

        try {
            List<FolderListItem> folders = listFolders(connection, false);

            if (!mStoreConfig.isSubscribedFoldersOnly()) {
                return getFolders(folders);
            }

            List<FolderListItem> subscribedFolders = listFolders(connection, true);

            List<FolderListItem> filteredFolders = limitToSubscribedFolders(folders, subscribedFolders);
            return getFolders(filteredFolders);
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
            subscribedFolderNames.add(subscribedFolder.getName());
        }

        List<FolderListItem> filteredFolders = new ArrayList<>();
        for (FolderListItem folder : folders) {
            if (subscribedFolderNames.contains(folder.getName())) {
                filteredFolders.add(folder);
            }
        }

        return filteredFolders;
    }

    private List<FolderListItem> listFolders(ImapConnection connection, boolean subscribedOnly) throws IOException,
            MessagingException {

        String command;
        if (subscribedOnly) {
            command = "LSUB";
        } else if (connection.hasCapability(Capabilities.SPECIAL_USE)) {
            command = "LIST (SPECIAL-USE)";
        } else {
            command = "LIST";
        }

        String encodedListPrefix = ImapUtility.encodeString(getCombinedPrefix() + "*");
        List<ImapResponse> responses = connection.executeSimpleCommand(
                String.format("%s \"\" %s", command, encodedListPrefix));

        List<ListResponse> listResponses = (subscribedOnly) ?
                ListResponse.parseLsub(responses) :
                ListResponse.parseList(responses);

        List<FolderListItem> folders = new ArrayList<>(listResponses.size());
        for (ListResponse listResponse : listResponses) {
            String decodedFolderName;
            try {
                decodedFolderName = folderNameCodec.decode(listResponse.getName());
            } catch (CharacterCodingException e) {
                Timber.w(e, "Folder name not correctly encoded with the UTF-7 variant as defined by RFC 3501: %s",
                        listResponse.getName());

                //TODO: Use the raw name returned by the server for all commands that require
                //      a folder name. Use the decoded name only for showing it to the user.

                // We currently just skip folders with malformed names.
                continue;
            }

            String folder = decodedFolderName;

            if (pathDelimiter == null) {
                pathDelimiter = listResponse.getHierarchyDelimiter();
                combinedPrefix = null;
            }

            if (ImapFolder.INBOX.equalsIgnoreCase(folder)) {
                continue;
            } else if (folder.equals(mStoreConfig.getOutboxFolder())) {
                /*
                 * There is a folder on the server with the same name as our local
                 * outbox. Until we have a good plan to deal with this situation
                 * we simply ignore the folder on the server.
                 */
                continue;
            } else if (listResponse.hasAttribute("\\NoSelect")) {
                continue;
            }

            folder = removePrefixFromFolderName(folder);
            if (folder == null) {
                continue;
            }

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

            folders.add(new FolderListItem(folder, type));
        }

        folders.add(new FolderListItem(ImapFolder.INBOX, FolderType.INBOX));

        return folders;
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

    @Override
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
                mTrustedSocketFactory,
                connectivityManager,
                oauthTokenProvider);
    }

    FolderNameCodec getFolderNameCodec() {
        return folderNameCodec;
    }

    private List<ImapFolder> getFolders(List<FolderListItem> folders) {
        List<ImapFolder> imapFolders = new ArrayList<>(folders.size());

        for (FolderListItem folder : folders) {
            ImapFolder imapFolder = getFolder(folder.getName());
            imapFolder.setType(folder.getType());
            imapFolders.add(imapFolder);
        }

        return imapFolders;
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
