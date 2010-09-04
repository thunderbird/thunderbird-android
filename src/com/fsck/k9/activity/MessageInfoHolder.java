package com.fsck.k9.activity;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Config;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingController.SORT_TYPE;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;

public class MessageInfoHolder implements Comparable<MessageInfoHolder>
{
    public String subject;
    public String date;
    public String fullDate;
    public Date compareDate;
    public String compareSubject;
    public CharSequence sender;
    public String senderAddress;
    public String compareCounterparty;
    public String preview;
    public String[] recipients;
    public boolean hasAttachments;
    public String uid;
    public boolean read;
    public boolean answered;
    public boolean flagged;
    public boolean downloaded;
    public boolean partially_downloaded;
    public Message message;
    public FolderInfoHolder folder;
    public boolean selected;
    public String account;
    public String uri;

    private Contacts mContacts;

    private SORT_TYPE sortType = SORT_TYPE.SORT_DATE;

    private boolean sortAscending = true;
    private boolean sortDateAscending = false;
    private MessagingController mController;

    // Empty constructor for comparison
    public MessageInfoHolder()
    {
        this.selected = false;
    }

    public MessageInfoHolder(Context context, Message m)
    {
        this();

        mContacts = Contacts.getInstance(context);

        Account account = m.getFolder().getAccount();
        mController = MessagingController.getInstance(K9.app);
        sortType = mController.getSortType();
        sortAscending = mController.isSortAscending(sortType);
        sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);
        populate(context, m, new FolderInfoHolder(context, m.getFolder(), m.getFolder().getAccount()), account);
    }

    public MessageInfoHolder(Context context ,Message m, SORT_TYPE t_sort, boolean asc)
    {
        this();

        mContacts = Contacts.getInstance(context);

        Account account = m.getFolder().getAccount();
        mController = MessagingController.getInstance(K9.app);
        sortType = t_sort;
        sortAscending = asc;
        sortDateAscending = asc;
        populate(context, m, new FolderInfoHolder(context, m.getFolder(), m.getFolder().getAccount()), account);
    }

    public MessageInfoHolder(Context context, Message m, FolderInfoHolder folder, Account account)
    {
        this();

        mContacts = Contacts.getInstance(context);

        mController = MessagingController.getInstance(K9.app);
        sortType = mController.getSortType();
        sortAscending = mController.isSortAscending(sortType);
        sortDateAscending = mController.isSortAscending(SORT_TYPE.SORT_DATE);
        populate(context, m, folder, account);

    }

    public void populate(Context context, Message m, FolderInfoHolder folder, Account account)
    {
        try
        {
            LocalMessage message = (LocalMessage) m;
            Date date = message.getSentDate();
            this.compareDate = message.getSentDate();
            if (this.compareDate == null)
            {
                this.compareDate = message.getInternalDate();
            }

            this.folder = folder;

            if (Utility.isDateToday(date))
            {
                this.date = android.text.format.DateFormat.getTimeFormat(context).format(date);
            }
            else
            {
                this.date = DateFormatter.getDateFormat(context).format(date);
            }

            this.hasAttachments = message.getAttachmentCount() > 0;

            this.read = message.isSet(Flag.SEEN);
            this.answered = message.isSet(Flag.ANSWERED);
            this.flagged = message.isSet(Flag.FLAGGED);
            this.downloaded = message.isSet(Flag.X_DOWNLOADED_FULL);
            this.partially_downloaded = message.isSet(Flag.X_DOWNLOADED_PARTIAL);

            Address[] addrs = message.getFrom();

            if (addrs.length > 0 &&  account.isAnIdentity(addrs[0]))
            {
                CharSequence to = Address.toFriendly(message .getRecipients(RecipientType.TO), mContacts);
                this.compareCounterparty = to.toString();
                this.sender = new SpannableStringBuilder(context.getString(R.string.message_list_to_fmt)).append(to);
            }
            else
            {
                this.sender = Address.toFriendly(addrs, mContacts);
                this.compareCounterparty = this.sender.toString();
            }

            if (addrs.length > 0)
            {
                this.senderAddress = addrs[0].getAddress();
            }
            else
            {
                // a reasonable fallback "whomever we were corresponding with
                this.senderAddress = this.compareCounterparty;
            }

            this.subject = message.getSubject();

            this.uid = message.getUid();
            this.message = m;
            this.preview = message.getPreview();

            this.fullDate = DateFormatter.getDateFormat(context).format(date)+" "+android.text.format.DateFormat.getTimeFormat(context).format(date);
            this.account = account.getDescription();
            this.uri = "email://messages/"+account.getAccountNumber()+"/"+m.getFolder().getName()+"/"+m.getUid();

        }
        catch (MessagingException me)
        {
            if (Config.LOGV)
            {
                Log.v(K9.LOG_TAG, "Unable to load message info", me);
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (o instanceof MessageInfoHolder == false)
        {
            return false;
        }
        MessageInfoHolder other = (MessageInfoHolder)o;
        return message.equals(other.message);
    }

    @Override
    public int hashCode()
    {
        return uid.hashCode();
    }

    public int compareTo(MessageInfoHolder o)
    {
        int ascender = (sortAscending ? 1 : -1);
        int comparison = 0;

        if (sortType == SORT_TYPE.SORT_SUBJECT)
        {
            if (compareSubject == null)
            {
                compareSubject = stripPrefixes(subject).toLowerCase();
            }

            if (o.compareSubject == null)
            {
                o.compareSubject = stripPrefixes(o.subject).toLowerCase();
            }

            comparison = this.compareSubject.compareTo(o.compareSubject);
        }
        else if (sortType == SORT_TYPE.SORT_SENDER)
        {
            comparison = this.compareCounterparty.toLowerCase().compareTo(o.compareCounterparty.toLowerCase());
        }
        else if (sortType == SORT_TYPE.SORT_FLAGGED)
        {
            comparison = (this.flagged ? 0 : 1) - (o.flagged ? 0 : 1);
        }
        else if (sortType == SORT_TYPE.SORT_UNREAD)
        {
            comparison = (this.read ? 1 : 0) - (o.read ? 1 : 0);
        }
        else if (sortType == SORT_TYPE.SORT_ATTACHMENT)
        {
            comparison = (this.hasAttachments ? 0 : 1) - (o.hasAttachments ? 0 : 1);
        }

        if (comparison != 0)
        {
            return comparison * ascender;
        }

        int dateAscender = (sortDateAscending ? 1 : -1);

        return this.compareDate.compareTo(o.compareDate) * dateAscender;
    }

    Pattern pattern = null;
    String patternString = "^ *(re|aw|fw|fwd): *";
    private String stripPrefixes(String in)
    {
        synchronized (patternString)
        {
            if (pattern == null)
            {
                pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
            }
        }

        Matcher matcher = pattern.matcher(in);

        int lastPrefix = -1;

        while (matcher.find())
        {
            lastPrefix = matcher.end();
        }

        if (lastPrefix > -1 && lastPrefix < in.length() - 1)
        {
            return in.substring(lastPrefix);
        }
        else
        {
            return in;
        }
    }
}
