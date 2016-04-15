package com.fsck.k9.mailstore;


import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final Message message;
    public final Part rootPart;
    public final String text;
    public final List<AttachmentViewInfo> attachments;
    public final CryptoResultAnnotation cryptoResultAnnotation;


    public MessageViewInfo(Message message, Part rootPart, String text, List<AttachmentViewInfo> attachments,
            CryptoResultAnnotation cryptoResultAnnotation) {
        this.message = message;
        this.rootPart = rootPart;
        this.text = text;
        this.attachments = attachments;
        this.cryptoResultAnnotation = cryptoResultAnnotation;
    }
}
