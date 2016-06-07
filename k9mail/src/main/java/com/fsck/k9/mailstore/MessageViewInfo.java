package com.fsck.k9.mailstore;


import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final Message message;
    public final Part rootPart;
    public final String text;
    public final CryptoResultAnnotation cryptoResultAnnotation;
    public final List<AttachmentViewInfo> attachments;
    public final String extraText;
    public final List<AttachmentViewInfo> extraAttachments;


    private MessageViewInfo(Message message, Part rootPart, String text, List<AttachmentViewInfo> attachments,
            CryptoResultAnnotation cryptoResultAnnotation, String extraText,
            List<AttachmentViewInfo> extraAttachments) {
        this.message = message;
        this.rootPart = rootPart;
        this.text = text;
        this.cryptoResultAnnotation = cryptoResultAnnotation;
        this.attachments = attachments;
        this.extraText = extraText;
        this.extraAttachments = extraAttachments;
    }

    public static MessageViewInfo createWithExtractedContent(Message message, Part rootPart,
            String text, List<AttachmentViewInfo> attachments, CryptoResultAnnotation cryptoResultAnnotation,
            String extraText, List<AttachmentViewInfo> extraAttachments) {
        return new MessageViewInfo(message, rootPart, text, attachments, cryptoResultAnnotation,
                extraText, extraAttachments);
    }

    public static MessageViewInfo createWithErrorState(Message message) {
        return new MessageViewInfo(message, null, null, null, null, null, null);
    }

}
