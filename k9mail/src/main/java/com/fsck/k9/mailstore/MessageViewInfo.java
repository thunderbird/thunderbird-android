package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Message;
import org.openintents.openpgp.OpenPgpSignatureResult;


public class MessageViewInfo {

    public final Message message;
    public final List<MessageViewContainer> containers = new ArrayList<MessageViewContainer>();

    @Deprecated
    public MessageViewInfo(String text, List<AttachmentViewInfo> attachments, Message message) {
        containers.add(new MessageViewContainer(text, attachments));
        // FIXME just display it twice, for testing only
        containers.add(new MessageViewContainer(text, attachments));
        this.message = message;
    }

    public static class MessageViewContainer {

        final public String text;
        final public List<AttachmentViewInfo> attachments;
        final public OpenPgpSignatureResult signatureResult;

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = null;
        }

        MessageViewContainer(String text, List<AttachmentViewInfo> attachments,
                OpenPgpSignatureResult signatureResult) {
            this.text = text;
            this.attachments = attachments;
            this.signatureResult = signatureResult;
        }

    }

}
