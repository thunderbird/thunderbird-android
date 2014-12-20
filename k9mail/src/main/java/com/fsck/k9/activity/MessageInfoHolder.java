package com.fsck.k9.activity;

import java.util.Date;

import com.fsck.k9.mailstore.LocalMessage;

public class MessageInfoHolder {
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
