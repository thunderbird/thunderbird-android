package com.fsck.k9.mailstore;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final String text;
    public final List<Part> attachments;
    public final Message message;

    public MessageViewInfo(String text, List<Part> attachments, Message message) {
        this.text = text;
        this.attachments = Collections.unmodifiableList(attachments);
        this.message = message;
    }
}
