package com.fsck.k9.activity;
import com.fsck.k9.helper.MessageHelper;
import java.util.Date;

import com.fsck.k9.mail.Message;

public class MessageInfoHolder
{
    public String subject;
    public String date;
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
    public boolean dirty;
    public Message message;
    public FolderInfoHolder folder;
    public boolean selected;
    public String account;
    public String uri;

    // Empty constructor for comparison
    public MessageInfoHolder()
    {
        this.selected = false;
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

    public String getDate(MessageHelper messageHelper) {
        if (date == null) {
            date = messageHelper.formatDate(message.getSentDate());
        }
        return date;
    }
}
