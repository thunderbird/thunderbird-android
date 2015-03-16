package com.fsck.k9.mailstore;


import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final Message message;
    public final List<MessageViewContainer> containers;


    public MessageViewInfo(List<MessageViewContainer> containers, Message message) {
        this.containers = containers;
        this.message = message;
    }


    public static class MessageViewContainer {
        public final String text;
        public final Part rootPart;
        public final List<AttachmentViewInfo> attachments;
        public final OpenPgpResultAnnotation cryptoAnnotation;


        MessageViewContainer(String text, Part rootPart, List<AttachmentViewInfo> attachments,
                OpenPgpResultAnnotation cryptoAnnotation) {
            this.text = text;
            this.rootPart = rootPart;
            this.attachments = attachments;
            this.cryptoAnnotation = cryptoAnnotation;
        }
    }
}
