package com.fsck.k9.activity;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;


public class FolderInfoHolder implements Comparable<FolderInfoHolder> {
    public String serverId;
    public String displayName;
    public long lastChecked;
    public int unreadMessageCount = -1;
    public int flaggedMessageCount = -1;
    public boolean loading;
    public String status;
    public boolean lastCheckFailed;
    public Folder folder;
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

    public FolderInfoHolder(Context context, LocalFolder folder, Account account) {
        if (context == null) {
            throw new IllegalArgumentException("null context given");
        }
        populate(context, folder, account);
    }

    public FolderInfoHolder(Context context, LocalFolder folder, Account account, int unreadCount) {
        populate(context, folder, account, unreadCount);
    }

    public void populate(Context context, LocalFolder folder, Account account, int unreadCount) {
        populate(context, folder, account);
        this.unreadMessageCount = unreadCount;
        folder.close();

    }


    public void populate(Context context, LocalFolder folder, Account account) {
        this.folder = folder;
        this.serverId = folder.getServerId();
        this.lastChecked = folder.getLastUpdate();

        this.status = truncateStatus(folder.getStatus());

        this.displayName = getDisplayName(context, account, serverId, folder.getName());
        setMoreMessagesFromFolder(folder);
    }

    /**
     * Returns the display name for a folder.
     *
     * <p>
     * This will return localized strings for special folders like the Inbox or the Trash folder.
     * </p>
     */
    public static String getDisplayName(Context context, Account account, String serverId, String name) {
        final String displayName;
        if (serverId.equals(account.getSpamFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_spam_fmt, serverId);
        } else if (serverId.equals(account.getArchiveFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_archive_fmt, serverId);
        } else if (serverId.equals(account.getSentFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_sent_fmt, serverId);
        } else if (serverId.equals(account.getTrashFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_trash_fmt, serverId);
        } else if (serverId.equals(account.getDraftsFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_drafts_fmt, serverId);
        } else if (serverId.equals(account.getOutboxFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_outbox);
        } else if (serverId.equals(account.getInboxFolder())) {
            displayName = context.getString(R.string.special_mailbox_name_inbox);
        } else {
            displayName = name;
        }

        return displayName;
    }

    public void setMoreMessagesFromFolder(LocalFolder folder) {
        moreMessages = folder.hasMoreMessages();
    }
}
