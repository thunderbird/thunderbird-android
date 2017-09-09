package com.fsck.k9.activity;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;
import timber.log.Timber;


public class FolderInfoHolder implements Comparable<FolderInfoHolder> {
    public String id;
    public String name;
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
        return o instanceof FolderInfoHolder && id.equals(((FolderInfoHolder) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(FolderInfoHolder o) {
        String s1 = this.id;
        String s2 = o.id;

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
        this.id = folder.getId();
        this.name = folder.getName();
        this.lastChecked = folder.getLastUpdate();

        this.status = truncateStatus(folder.getStatus());

        this.displayName = getDisplayName(context, account, id, name);
        setMoreMessagesFromFolder(folder);
    }

    /**
     * Returns the display name for a folder.
     *
     * <p>
     * This will return localized strings for special folders like the Inbox or the Trash folder.
     * </p>
     *
     * @param context
     *         A {@link Context} instance that is used to get the string resources.
     * @param account
     *         The {@link Account} the folder belongs to.
     * @param id
     *         The id of the folder for which to return the display name.
     * @param name
     *         The name of the folder for which to return the display name.
     *
     * @return The localized id for the provided folder if it's a special folder or the original
     *         folder id if it's a non-special folder.
     */
    public static String getDisplayName(Context context, Account account, String id, String name) {
        final String displayName;
        if (id.equals(account.getSpamFolderId())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_spam_fmt), name);
        } else if (id.equals(account.getArchiveFolderId())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_archive_fmt), name);
        } else if (id.equals(account.getSentFolderId())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_sent_fmt), name);
        } else if (id.equals(account.getTrashFolderId())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_trash_fmt), name);
        } else if (id.equals(account.getDraftsFolderId())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_drafts_fmt), name);
        } else if (id.equals(account.getOutboxFolderId())) {
            displayName = context.getString(R.string.special_mailbox_name_outbox);
        // FIXME: We really shouldn't do a case-insensitive comparison here
        } else if (id.equalsIgnoreCase(account.getInboxFolderId())) {
            displayName = context.getString(R.string.special_mailbox_name_inbox);
        } else if (name == null) {
            displayName = id;
        } else {
            displayName = name;
        }

        return displayName;
    }

    public void setMoreMessagesFromFolder(LocalFolder folder) {
        moreMessages = folder.hasMoreMessages();
    }
}
