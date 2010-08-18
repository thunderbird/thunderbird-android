package com.fsck.k9.activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.style.TextAppearanceSpan;
import android.util.Config;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.format.DateFormat;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.FontSizes;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.SearchSpecification;
import com.fsck.k9.activity.setup.AccountSettings;
import com.fsck.k9.activity.setup.FolderSettings;
import com.fsck.k9.activity.setup.Prefs;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingController.SORT_TYPE;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore;
import com.fsck.k9.mail.store.LocalStore.LocalFolder;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;

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

    /**
     * Outbox is handled differently from any other folder.
     */
    public boolean outbox;

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
        populate(context, folder, account);
    }

    public FolderInfoHolder(Context context, Folder folder, Account mAccount, int unreadCount)
    {
        populate(context, folder, mAccount, unreadCount);
    }

    public void populate(Context context, Folder folder, Account mAccount, int unreadCount)
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

        this.name = folder.getName();

        if (this.name.equalsIgnoreCase(K9.INBOX))
        {
            this.displayName = context.getString(R.string.special_mailbox_name_inbox);
        }
        else
        {
            this.displayName = folder.getName();
        }

        if (this.name.equals(mAccount.getOutboxFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_outbox_fmt), this.name);
            this.outbox = true;
        }

        if (this.name.equals(mAccount.getDraftsFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_drafts_fmt), this.name);
        }

        if (this.name.equals(mAccount.getTrashFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_trash_fmt), this.name);
        }

        if (this.name.equals(mAccount.getSentFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_sent_fmt), this.name);
        }

        if (this.name.equals(mAccount.getArchiveFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_archive_fmt), this.name);
        }

        if (this.name.equals(mAccount.getSpamFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_spam_fmt), this.name);
        }

        this.lastChecked = folder.getLastUpdate();

        String mess = truncateStatus(folder.getStatus());

        this.status = mess;

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

        if (this.name.equalsIgnoreCase(K9.INBOX))
        {
            this.displayName = context.getString(R.string.special_mailbox_name_inbox);
        }
        else
        {
            this.displayName = this.name;
        }

        if (this.name.equals(account.getOutboxFolderName()))
        {
            this.displayName = String.format(context.getString(R.string.special_mailbox_name_outbox_fmt), this.name);
            this.outbox = true;
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
