package com.fsck.k9.activity;

import android.content.Context;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;

public class FolderInfoHolder implements Comparable<FolderInfoHolder>
{
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
    public boolean equals(Object o)
    {
        return this.name.equals(((FolderInfoHolder)o).name);
    }

    @Override
    public int hashCode()
    {
        return name.hashCode();
    }

    public int compareTo(FolderInfoHolder o)
    {
        String s1 = this.name;
        String s2 = o.name;

        int ret = s1.compareToIgnoreCase(s2);
        if (ret != 0)
        {
            return ret;
        }
        else
        {
            return s1.compareTo(s2);
        }

    }

    private String truncateStatus(String mess)
    {
        if (mess != null && mess.length() > 27)
        {
            mess = mess.substring(0, 27);
        }
        return mess;
    }

    // constructor for an empty object for comparisons
    public FolderInfoHolder()
    {
    }

    public FolderInfoHolder(Context context, Folder folder, Account account)
    {
        if (context == null)
        {
            throw new IllegalArgumentException("null context given");
        }
        populate(context, folder, account);
    }

    public FolderInfoHolder(Context context, Folder folder, Account account, int unreadCount)
    {
        populate(context, folder, account, unreadCount);
    }

    public void populate(Context context, Folder folder, Account account, int unreadCount)
    {

        try
        {
            folder.open(Folder.OpenMode.READ_WRITE);
            //  unreadCount = folder.getUnreadMessageCount();
        }
        catch (MessagingException me)
        {
            Log.e(K9.LOG_TAG, "Folder.getUnreadMessageCount() failed", me);
        }

        populate(context,folder,account);

        this.unreadMessageCount = unreadCount;

        try
        {
            this.flaggedMessageCount = folder.getFlaggedMessageCount();
        }
        catch (Exception e)
        {
            Log.e(K9.LOG_TAG, "Unable to get flaggedMessageCount", e);
        }

        folder.close();

    }


    public void populate(Context context, Folder folder, Account account)
    {
        this.folder = folder;
        this.name = folder.getName();
        this.lastChecked = folder.getLastUpdate();

        String mess = truncateStatus(folder.getStatus());

        this.status = mess;

        if (this.name.equalsIgnoreCase(K9.INBOX))
        {
            this.displayName = context.getString(R.string.special_mailbox_name_inbox);
        }
        else
        {
            this.displayName = folder.getName();
        }

        if (this.name.equals(account.getOutboxFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_outbox_fmt), this.name);
        }

        if (this.name.equals(account.getDraftsFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_drafts_fmt), this.name);
        }

        if (this.name.equals(account.getTrashFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_trash_fmt), this.name);
        }

        if (this.name.equals(account.getSentFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_sent_fmt), this.name);
        }

        if (this.name.equals(account.getArchiveFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_archive_fmt), this.name);
        }

        if (this.name.equals(account.getSpamFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_spam_fmt), this.name);
        }
    }
}
