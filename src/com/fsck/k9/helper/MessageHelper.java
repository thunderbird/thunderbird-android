package com.fsck.k9.helper;

import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.DateFormatter;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.activity.MessageInfoHolder;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.store.LocalStore.LocalMessage;

public class MessageHelper
{

    private static MessageHelper sInstance;

    public synchronized static MessageHelper getInstance(final Context context)
    {
        if (sInstance == null)
        {
            sInstance = new MessageHelper(context);
        }
        return sInstance;
    }

    private Context mContext;

    private DateFormat mTodayDateFormat;

    private DateFormat mDateFormat;

    private DateFormat mTimeFormat;

    private MessageHelper(final Context context)
    {
        mContext = context;
        mDateFormat = DateFormatter.getDateFormat(mContext);
        mTimeFormat = android.text.format.DateFormat.getTimeFormat(mContext);
        mTodayDateFormat = android.text.format.DateFormat.getTimeFormat(mContext);
    }

    public void populate(final MessageInfoHolder target, final Message m,
                         final FolderInfoHolder folder, final Account account)
    {
        final Contacts contactHelper = K9.showContactName() ? Contacts.getInstance(mContext) : null;
        try
        {
            LocalMessage message = (LocalMessage) m;
            target.message = message;
            Date date = message.getSentDate();
            target.compareDate = message.getSentDate();
            if (target.compareDate == null)
            {
                target.compareDate = message.getInternalDate();
            }

            target.folder = folder;

            target.read = message.isSet(Flag.SEEN);
            target.answered = message.isSet(Flag.ANSWERED);
            target.flagged = message.isSet(Flag.FLAGGED);
            target.downloaded = message.isSet(Flag.X_DOWNLOADED_FULL);
            target.partially_downloaded = message.isSet(Flag.X_DOWNLOADED_PARTIAL);

            Address[] addrs = message.getFrom();

            if (addrs.length > 0 &&  account.isAnIdentity(addrs[0]))
            {
                CharSequence to = Address.toFriendly(message .getRecipients(RecipientType.TO), contactHelper);
                target.compareCounterparty = to.toString();
                target.sender = new SpannableStringBuilder(mContext.getString(R.string.message_to_label)).append(to);
            }
            else
            {
                target.sender = Address.toFriendly(addrs, contactHelper);
                target.compareCounterparty = target.sender.toString();
            }

            if (addrs.length > 0)
            {
                target.senderAddress = addrs[0].getAddress();
            }
            else
            {
                // a reasonable fallback "whomever we were corresponding with
                target.senderAddress = target.compareCounterparty;
            }



            for (Address address : message.getRecipients(RecipientType.TO))
            {
                if (account.isAnIdentity(address))
                {
                    target.toMe = true;
                }
            }

            if (target.toMe == false )
            {
                for(Address address : message.getRecipients(RecipientType.CC))
                {
                    if (account.isAnIdentity(address))
                    {
                        target.ccMe = true;
                    }
                }
            }


            target.uid = message.getUid();

            target.account = account.getDescription();
            target.uri = "email://messages/" + account.getAccountNumber() + "/" + m.getFolder().getName() + "/" + m.getUid();

        }
        catch (MessagingException me)
        {
            Log.w(K9.LOG_TAG, "Unable to load message info", me);
        }
    }
    public String formatDate(Date date)
    {
        if (Utility.isDateToday(date))
        {
            return mTodayDateFormat.format(date);
        }
        else
        {
            return mDateFormat.format(date);
        }
    }
}
