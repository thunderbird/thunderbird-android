package com.fsck.k9.activity;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;

public class FolderInfoHolder implements Comparable<FolderInfoHolder> {
    public String name;
    public String displayName;
    public long lastChecked;
    public int unreadMessageCount;
    public int flaggedMessageCount;
    public boolean loading;
    public String status;
    public boolean lastCheckFailed;
    public Folder folder;
    public boolean pushActive;

    @Override
    public boolean equals(Object o) {
        return this.name.equals(((FolderInfoHolder)o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public int compareTo(FolderInfoHolder o) {
        String s1 = this.name;
        String s2 = o.name;

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

    public FolderInfoHolder(Context context, Folder folder, Account account) {
        if (context == null) {
            throw new IllegalArgumentException("null context given");
        }
        populate(context, folder, account);
    }

    public FolderInfoHolder(Context context, Folder folder, Account account, int unreadCount) {
        populate(context, folder, account, unreadCount);
    }

    public void populate(Context context, Folder folder, Account account, int unreadCount) {

        try {
            folder.open(Folder.OpenMode.READ_WRITE);
            //  unreadCount = folder.getUnreadMessageCount();
        } catch (MessagingException me) {
            Log.e(K9.LOG_TAG, "Folder.getUnreadMessageCount() failed", me);
        }

        populate(context, folder, account);

        this.unreadMessageCount = unreadCount;

        try {
            this.flaggedMessageCount = folder.getFlaggedMessageCount();
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Unable to get flaggedMessageCount", e);
        }

        folder.close();

    }


    public void populate(Context context, Folder folder, Account account) {
        this.folder = folder;
        this.name = folder.getName();
        this.lastChecked = folder.getLastUpdate();

        this.status = truncateStatus(folder.getStatus());

        this.displayName = getDisplayName(context, folder);
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
     * @param folder
     *         The {@link Folder} instance for which to return the display name.
     *
     * @return The localized name for the provided folder if it's a special folder or the original
     *         folder name if it's a non-special folder.
     */
    public static String getDisplayName(Context context, Folder folder) {
        Account account = folder.getAccount();
        String name = folder.getName();

        final String displayName;
        if (name.equals(account.getSpamFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_spam_fmt), name);
        } else if (name.equals(account.getArchiveFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_archive_fmt), name);
        } else if (name.equals(account.getSentFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_sent_fmt), name);
        } else if (name.equals(account.getTrashFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_trash_fmt), name);
        } else if (name.equals(account.getDraftsFolderName())) {
            displayName = String.format(
                    context.getString(R.string.special_mailbox_name_drafts_fmt), name);
        } else if (name.equals(account.getOutboxFolderName())) {
            displayName = context.getString(R.string.special_mailbox_name_outbox);
        // FIXME: We really shouldn't do a case-insensitive comparison here
        } else if (name.equalsIgnoreCase(account.getInboxFolderName())) {
            displayName = context.getString(R.string.special_mailbox_name_inbox);
        } else {
            displayName = name;
        }

        return displayName;
    }
}
