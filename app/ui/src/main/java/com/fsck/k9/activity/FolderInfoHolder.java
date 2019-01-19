package com.fsck.k9.activity;


import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.mailstore.FolderType;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.ui.folders.FolderNameFormatter;


public class FolderInfoHolder implements Comparable<FolderInfoHolder> {
    private final FolderNameFormatter folderNameFormatter = DI.get(FolderNameFormatter.class);

    public String serverId;
    public String displayName;
    public long lastChecked;
    public int unreadMessageCount = -1;
    public int flaggedMessageCount = -1;
    public boolean loading;
    public String status;
    public boolean lastCheckFailed;
    public LocalFolder folder;
    public boolean pushActive;
    public boolean moreMessages;

    @Override
    public boolean equals(Object o) {
        return o instanceof FolderInfoHolder && serverId.equals(((FolderInfoHolder) o).serverId);
    }

    @Override
    public int hashCode() {
        return serverId.hashCode();
    }

    public int compareTo(FolderInfoHolder o) {
        String s1 = this.serverId;
        String s2 = o.serverId;

        int ret = s1.compareToIgnoreCase(s2);
        if (ret != 0) {
            return ret;
        } else {
            return s1.compareTo(s2);
        }

    }

    private String truncateStatus(String mess) {
        if (mess != null && mess.length() > 27) {
            mess = mess.substring(0, 27);
        }
        return mess;
    }

    // constructor for an empty object for comparisons
    public FolderInfoHolder() {
    }

    public FolderInfoHolder(LocalFolder folder, Account account) {
        populate(folder, account);
    }

    public FolderInfoHolder(LocalFolder folder, Account account, int unreadCount) {
        populate(folder, account, unreadCount);
    }

    public void populate(LocalFolder folder, Account account, int unreadCount) {
        populate(folder, account);
        this.unreadMessageCount = unreadCount;
        folder.close();
    }

    public void populate(LocalFolder localFolder, Account account) {
        this.folder = localFolder;
        this.serverId = localFolder.getServerId();
        this.lastChecked = localFolder.getLastUpdate();

        this.status = truncateStatus(localFolder.getStatus());

        this.displayName = getDisplayName(account, localFolder);
        setMoreMessagesFromFolder(localFolder);
    }

    private String getDisplayName(Account account, LocalFolder localFolder) {
        String serverId = localFolder.getServerId();
        Folder folder = new Folder(
                localFolder.getDatabaseId(),
                serverId,
                localFolder.getName(),
                getFolderType(account, serverId));

        return folderNameFormatter.displayName(folder);
    }

    private static FolderType getFolderType(Account account, String serverId) {
        if (serverId.equals(account.getInboxFolder())) {
            return FolderType.INBOX;
        } else if (serverId.equals(account.getOutboxFolder())) {
            return FolderType.OUTBOX;
        } else if (serverId.equals(account.getArchiveFolder())) {
            return FolderType.ARCHIVE;
        } else if (serverId.equals(account.getDraftsFolder())) {
            return FolderType.DRAFTS;
        } else if (serverId.equals(account.getSentFolder())) {
            return FolderType.SENT;
        } else if (serverId.equals(account.getSpamFolder())) {
            return FolderType.SPAM;
        } else if (serverId.equals(account.getTrashFolder())) {
            return FolderType.TRASH;
        } else {
            return FolderType.REGULAR;
        }
    }

    /**
     * Returns the display name for a folder.
     *
     * Deprecated. Use {@link FolderNameFormatter} instead.
     */
    @Deprecated
    public static String getDisplayName(Account account, String serverId, String name) {
        FolderNameFormatter folderNameFormatter = DI.get(FolderNameFormatter.class);
        FolderType folderType = getFolderType(account, serverId);
        Folder folder = new Folder(-1, serverId, name, folderType);

        return folderNameFormatter.displayName(folder);
    }

    public void setMoreMessagesFromFolder(LocalFolder folder) {
        moreMessages = folder.hasMoreMessages();
    }
}
