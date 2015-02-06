package com.fsck.k9.mailstore;


import java.util.List;

import android.app.PendingIntent;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Part;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


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
        public final boolean encrypted;
        public final OpenPgpSignatureResult signatureResult;
        public final OpenPgpError pgpError;
        public final PendingIntent pgpPendingIntent;

        MessageViewContainer(String text, Part rootPart, List<AttachmentViewInfo> attachments) {
            this(text, rootPart, attachments, null, null, false, null);
        }

        MessageViewContainer(String text, Part rootPart, List<AttachmentViewInfo> attachments,
                OpenPgpSignatureResult signatureResult, OpenPgpError pgpError, boolean encrypted,
                PendingIntent pgpPendingIntent) {
            this.text = text;
            this.rootPart = rootPart;
            this.attachments = attachments;
            this.signatureResult = signatureResult;
            this.pgpError = pgpError;
            this.encrypted = encrypted;
            this.pgpPendingIntent = pgpPendingIntent;
        }
    }
}
