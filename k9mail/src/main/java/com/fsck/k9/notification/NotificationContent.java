package com.fsck.k9.notification;


import com.fsck.k9.activity.MessageReference;


class NotificationContent {
    public final MessageReference messageReference;
    public final String sender;
    public final String subject;
    public final CharSequence preview;
    public final CharSequence summary;
    public final boolean starred;
    public final boolean isHighPriority;


    public NotificationContent(MessageReference messageReference, String sender, String subject, CharSequence preview,
            CharSequence summary, boolean starred, boolean isHighPriority) {
        this.messageReference = messageReference;
        this.sender = sender;
        this.subject = subject;
        this.preview = preview;
        this.summary = summary;
        this.starred = starred;
        this.isHighPriority = isHighPriority;
    }
}
