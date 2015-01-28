package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Message;
import org.openintents.openpgp.OpenPgpSignatureResult;


public class MessageViewInfo {

    public final Message message;
    public final List<MessageViewContainer> containers;

    @Deprecated
    public MessageViewInfo(String text, List<AttachmentViewInfo> attachments, Message message) {
        containers = new ArrayList<MessageViewContainer>();
        containers.add(new MessageViewContainer(text, attachments));
        this.message = message;
    }

    public MessageViewInfo(List<MessageViewContainer> containers, Message message) {
        this.containers = containers;
        this.message = message;
    }

    public static class MessageViewContainer {

        final public String text;
        final public List<AttachmentViewInfo> attachments;
        final public boolean encrypted;
        final public OpenPgpSignatureResult signatureResult;
        final public PendingIntent pgpPendingIntent;

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = null;
            this.encrypted = false;
            this.pgpPendingIntent = null;
        }

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments,
                OpenPgpSignatureResult signatureResult, boolean encrypted,
                PendingIntent pgpPendingIntent) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = signatureResult;
            this.encrypted = encrypted;
            this.pgpPendingIntent = pgpPendingIntent;
        }

    }

}
