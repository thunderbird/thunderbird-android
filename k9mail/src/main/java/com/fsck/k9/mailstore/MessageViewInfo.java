package com.fsck.k9.mailstore;


import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;


public class MessageViewInfo {
    public final Message message;
    public final boolean isMessageIncomplete;
    public final Part rootPart;
    public final String subject;
    public final AttachmentResolver attachmentResolver;
    public final String text;
    public final CryptoResultAnnotation cryptoResultAnnotation;
    public final List<AttachmentViewInfo> attachments;
    public final String extraText;
    public final List<AttachmentViewInfo> extraAttachments;
    public final List<ICalendarViewInfo> iCalendarEvents;


    public MessageViewInfo(
            Message message, boolean isMessageIncomplete, Part rootPart,
            String subject,
            String text, List<AttachmentViewInfo> attachments,
            List<ICalendarViewInfo> iCalendarEvents,
            CryptoResultAnnotation cryptoResultAnnotation,
            AttachmentResolver attachmentResolver,
            String extraText, List<AttachmentViewInfo> extraAttachments) {
        this.message = message;
        this.isMessageIncomplete = isMessageIncomplete;
        this.rootPart = rootPart;
        this.subject = subject;
        this.text = text;
        this.cryptoResultAnnotation = cryptoResultAnnotation;
        this.attachmentResolver = attachmentResolver;
        this.attachments = attachments;
        this.iCalendarEvents = iCalendarEvents;
        this.extraText = extraText;
        this.extraAttachments = extraAttachments;
    }

    static MessageViewInfo createWithExtractedContent(Message message, Part rootPart, boolean isMessageIncomplete,
            String text, List<AttachmentViewInfo> attachments, AttachmentResolver attachmentResolver,
            List<ICalendarViewInfo> calendars) {
        return new MessageViewInfo(
                message, isMessageIncomplete, rootPart, null, text, attachments, calendars, null,
                attachmentResolver, null, Collections.<AttachmentViewInfo>emptyList());
    }

    public static MessageViewInfo createWithErrorState(Message message, boolean isMessageIncomplete) {
        return new MessageViewInfo(message, isMessageIncomplete, null, null, null, null, null, null, null, null, null);
    }

    public static MessageViewInfo createForMetadataOnly(Message message, boolean isMessageIncomplete) {
        return new MessageViewInfo(message, isMessageIncomplete, null, null, null, null, null, null, null, null, null);
    }

    MessageViewInfo withCryptoData(CryptoResultAnnotation rootPartAnnotation, String extraViewableText,
            List<AttachmentViewInfo> extraAttachmentInfos) {
        return new MessageViewInfo(
                message, isMessageIncomplete, rootPart, subject, text, attachments, iCalendarEvents,
                rootPartAnnotation,
                attachmentResolver,
                extraViewableText, extraAttachmentInfos
        );
    }

    MessageViewInfo withSubject(String subject) {
        return new MessageViewInfo(
                message, isMessageIncomplete, rootPart, subject, text, attachments,
                iCalendarEvents, cryptoResultAnnotation, attachmentResolver, extraText, extraAttachments
        );
    }
}
