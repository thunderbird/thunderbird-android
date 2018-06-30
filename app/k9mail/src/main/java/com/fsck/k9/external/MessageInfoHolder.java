package com.fsck.k9.external;

import java.util.Date;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.activity.FolderInfoHolder;
import com.fsck.k9.core.R;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MessageHelper;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mailstore.LocalMessage;

class MessageInfoHolder {
    public String date;
    public Date compareDate;
    public Date compareArrival;
    public String compareSubject;
    public CharSequence sender;
    public String senderAddress;
    public String compareCounterparty;
    public String[] recipients;
    public String uid;
    public boolean read;
    public boolean answered;
    public boolean forwarded;
    public boolean flagged;
    public boolean dirty;
    public LocalMessage message;
    public FolderInfoHolder folder;
    public boolean selected;
    public String account;
    public String uri;


    public static MessageInfoHolder create(Context context, LocalMessage message, FolderInfoHolder folder,
            Account account) {
        Contacts contactHelper = K9.showContactName() ? Contacts.getInstance(context) : null;

        MessageInfoHolder target = new MessageInfoHolder();

        target.message = message;
        target.compareArrival = message.getInternalDate();
        target.compareDate = message.getSentDate();
        if (target.compareDate == null) {
            target.compareDate = message.getInternalDate();
        }

        target.folder = folder;

        target.read = message.isSet(Flag.SEEN);
        target.answered = message.isSet(Flag.ANSWERED);
        target.forwarded = message.isSet(Flag.FORWARDED);
        target.flagged = message.isSet(Flag.FLAGGED);

        Address[] addrs = message.getFrom();

        if (addrs.length > 0 &&  account.isAnIdentity(addrs[0])) {
            CharSequence to = MessageHelper.toFriendly(message.getRecipients(RecipientType.TO), contactHelper);
            target.compareCounterparty = to.toString();
            target.sender = new SpannableStringBuilder(context.getString(R.string.message_to_label)).append(to);
        } else {
            target.sender = MessageHelper.toFriendly(addrs, contactHelper);
            target.compareCounterparty = target.sender.toString();
        }

        if (addrs.length > 0) {
            target.senderAddress = addrs[0].getAddress();
        } else {
            // a reasonable fallback "whomever we were corresponding with
            target.senderAddress = target.compareCounterparty;
        }

        target.uid = message.getUid();
        target.account = message.getFolder().getAccountUuid();
        target.uri = message.getUri();

        return target;
    }

    // Empty constructor for comparison
    public MessageInfoHolder() {
        this.selected = false;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MessageInfoHolder)) {
            return false;
        }
        MessageInfoHolder other = (MessageInfoHolder)o;
        return message.equals(other.message);
    }

    @Override
    public int hashCode() {
        return uid.hashCode();
    }
}
