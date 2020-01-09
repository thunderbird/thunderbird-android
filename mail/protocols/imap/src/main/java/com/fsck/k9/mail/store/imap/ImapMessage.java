package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.internet.MimeMessage;


public class ImapMessage extends MimeMessage {
    ImapMessage(String uid) {
        this.mUid = uid;
    }

    public void setSize(int size) {
        this.mSize = size;
    }
}
