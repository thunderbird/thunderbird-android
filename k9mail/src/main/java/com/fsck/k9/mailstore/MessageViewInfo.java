package com.fsck.k9.mailstore;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final Message message;
    public final boolean isMessageIncomplete;
    public final Part rootPart;
    public final AttachmentResolver attachmentResolver;
    public final String text;
    public final CryptoResultAnnotation cryptoResultAnnotation;
    public final List<AttachmentViewInfo> attachments;
    public final String extraText;
    public final List<AttachmentViewInfo> extraAttachments;


    public MessageViewInfo(
            Message message, boolean isMessageIncomplete, Part rootPart,
            String text, List<AttachmentViewInfo> attachments,
            CryptoResultAnnotation cryptoResultAnnotation,
            AttachmentResolver attachmentResolver,
            String extraText, List<AttachmentViewInfo> extraAttachments) {
        this.message = message;
        this.isMessageIncomplete = isMessageIncomplete;
        this.rootPart = rootPart;
        this.text = text;
        this.cryptoResultAnnotation = cryptoResultAnnotation;
        this.attachmentResolver = attachmentResolver;
        this.attachments = attachments;
        this.extraText = extraText;
        this.extraAttachments = extraAttachments;
    }

    static MessageViewInfo createWithExtractedContent(Message message, Part rootPart, boolean isMessageIncomplete,
            String text, List<AttachmentViewInfo> attachments, AttachmentResolver attachmentResolver) {
        return new MessageViewInfo(
                message, isMessageIncomplete, rootPart, text, attachments, null, attachmentResolver, null,
                Collections.<AttachmentViewInfo>emptyList());
    }

    public static MessageViewInfo createWithErrorState(Message message, boolean isMessageIncomplete) {
        return new MessageViewInfo(message, isMessageIncomplete, null, null, null, null, null, null, null);
    }

    public static MessageViewInfo createForMetadataOnly(Message message, boolean isMessageIncomplete) {
        return new MessageViewInfo(message, isMessageIncomplete, null, null, null, null, null, null, null);
    }

    MessageViewInfo withCryptoData(CryptoResultAnnotation rootPartAnnotation, String extraViewableText,
            List<AttachmentViewInfo> extraAttachmentInfos) {
        return new MessageViewInfo(
                message, isMessageIncomplete, rootPart, text, attachments,
                rootPartAnnotation,
                attachmentResolver,
                extraViewableText, extraAttachmentInfos
        );
    }
}
