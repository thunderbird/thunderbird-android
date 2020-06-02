package com.fsck.k9.mail.store.pop3;


import com.fsck.k9.mail.internet.MimeMessage;


public class Pop3Message extends MimeMessage {
    Pop3Message(String uid) {
        mUid = uid;
        mSize = -1;
    }

    public void setSize(int size) {
        mSize = size;
    }
}
