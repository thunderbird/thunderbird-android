package com.fsck.k9.notification;


import com.fsck.k9.activity.MessageReference;
import com.fsck.k9.mail.Address;


class NotificationContent {
    public final MessageReference messageReference;
    public final String sender;
    public final Address senderNameAddress;
    public final String subject;
    public final CharSequence preview;
    public final CharSequence summary;
    public final boolean starred;


    public NotificationContent(MessageReference messageReference, String sender, Address senderNameAddress, String subject, CharSequence preview,
            CharSequence summary, boolean starred) {
        this.messageReference = messageReference;
        this.sender = sender;
        this.senderNameAddress = senderNameAddress;
        this.subject = subject;
        this.preview = preview;
        this.summary = summary;
        this.starred = starred;
    }
}
