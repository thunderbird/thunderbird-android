package com.fsck.k9.activity;


import com.fsck.k9.Account;
import com.fsck.k9.mailstore.Folder;
import com.fsck.k9.mailstore.FolderType;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.ui.folders.FolderNameFormatter;


public class FolderInfoHolder {
    private final FolderNameFormatter folderNameFormatter;

    public final String serverId;
    public final String displayName;
    public final long lastChecked;
    public boolean loading;
    public boolean moreMessages;


    public FolderInfoHolder(FolderNameFormatter folderNameFormatter, LocalFolder localFolder, Account account) {
        this.folderNameFormatter = folderNameFormatter;
        this.serverId = localFolder.getServerId();
        this.lastChecked = localFolder.getLastUpdate();
        this.displayName = getDisplayName(account, localFolder);
        moreMessages = localFolder.hasMoreMessages();
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

    public static FolderType getFolderType(Account account, String serverId) {
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
}
