package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Message;
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

        final public String text;
        final public List<AttachmentViewInfo> attachments;
        final public boolean encrypted;
        final public OpenPgpSignatureResult signatureResult;
        final public OpenPgpError pgpError;
        final public PendingIntent pgpPendingIntent;

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = null;
            this.pgpError = null;
            this.encrypted = false;
            this.pgpPendingIntent = null;
        }

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments,
                OpenPgpSignatureResult signatureResult, OpenPgpError pgpError,
                boolean encrypted, PendingIntent pgpPendingIntent) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = signatureResult;
            this.pgpError = pgpError;
            this.encrypted = encrypted;
            this.pgpPendingIntent = pgpPendingIntent;
        }

    }

}
