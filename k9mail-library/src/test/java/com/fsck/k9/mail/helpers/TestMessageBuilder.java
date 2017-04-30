package com.fsck.k9.mail.helpers;


import com.fsck.k9.mail.Message;


public class TestMessageBuilder {
    String from;
    String to;
    boolean hasAttachments;
    long messageSize;


    public TestMessageBuilder from(String email) {
        from = email;
        return this;
    }

    public TestMessageBuilder to(String email) {
        to = email;
        return this;
    }

    public TestMessageBuilder setHasAttachments(boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
        return this;
    }

    public TestMessageBuilder messageSize(long messageSize) {
        this.messageSize = messageSize;
        return this;
    }
    
    public Message build() {
        return new TestMessage(this);
    }
}
