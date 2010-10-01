package com.fsck.k9.activity;
import java.util.Date;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Config;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;

public class MessageInfoHolder
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
        populate(context, m, new FolderInfoHolder(context, m.getFolder(), m.getFolder().getAccount()), account);
    }

    public MessageInfoHolder(Context context, Message m, FolderInfoHolder folder, Account account)
    {
        this();

        mContacts = Contacts.getInstance(context);

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

}
