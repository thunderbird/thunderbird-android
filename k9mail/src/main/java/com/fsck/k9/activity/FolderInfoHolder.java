package com.fsck.k9.activity;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.R;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mailstore.LocalFolder;


public class FolderInfoHolder implements Comparable<FolderInfoHolder> {
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
        return o instanceof FolderInfoHolder && name.equals(((FolderInfoHolder) o).name);
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
        this.name = folder.getName();
        this.lastChecked = folder.getLastUpdate();

        this.status = formatStatus(context, folder.getStatus());

        this.displayName = getDisplayName(context, account, name);
        setMoreMessagesFromFolder(folder);
    }

    /**
     * Translate a folder status (which can be an opaque exception in error cases) into a
     * user-friendly, translated string where possible.
     *
     * Truncate non-translated strings to 27 characters.
     *
     * @return formatted status
     */
    private String formatStatus(Context context, String mess) {
        if (mess != null) {
            if (mess.startsWith(MessagingController.PUSH_FAILED_ERROR_PREFIX)) {
                String remainder = formatStatus(context, mess.substring(
                        MessagingController.PUSH_FAILED_ERROR_PREFIX.length()));
                String.format(context.getString(R.string.folder_error_push_failed), remainder);
            }

            if (mess.equals("SocketException: Socket is closed") ||
                    mess.contains("Connection reset by peer")) {
                    //SSLException: Read error: ... I/O error during system call, Connection reset by peer - closed
                return context.getString(R.string.folder_error_remote_socket_closed);
            } else if (mess.startsWith("GaiException: android_getaddrinfo failed:") ||
                    mess.startsWith("UnknownHostException: Unable to resolve host")) {
                return context.getString(R.string.folder_error_hostname_lookup_failed);
            } else if (mess.startsWith("SocketTimeoutException")) {
                return context.getString(R.string.folder_error_connection_attempt_failed);
            } else if (mess.startsWith("ErrnoException: open failed: ENOENT")) {
                return context.getString(R.string.folder_error_open_failed_enoent);
            } else if (mess.contains("Connection timed out")) {
                //SSLException: Read error: ssl=0x7f7392e200: I/O error during system call, Connection timed out
                return context.getString(R.string.folder_error_connnection_timed_out);
            }

            if (mess.length() > 27) {
                mess = mess.substring(0, 27);
            }
        }
        return mess;
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
     * @param name
     *         The name of the folder for which to return the display name.
     *
     * @return The localized name for the provided folder if it's a special folder or the original
     *         folder name if it's a non-special folder.
     */
    public static String getDisplayName(Context context, Account account, String name) {
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

    public void setMoreMessagesFromFolder(LocalFolder folder) {
        moreMessages = folder.hasMoreMessages();
    }
}
