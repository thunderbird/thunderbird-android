package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.Viewable;


class MessageInfoExtractor {
    private final Context context;
    private final Message message;
    private List<Viewable> viewables;
    private List<Part> attachments;

    public MessageInfoExtractor(Context context, Message message) {
        this.context = context;
        this.message = message;
    }

    public String getMessageTextPreview() throws MessagingException {
        getViewablesIfNecessary();
        return MessagePreviewExtractor.extractPreview(context, viewables);
    }

    public int getAttachmentCount() throws MessagingException {
        getViewablesIfNecessary();
        return attachments.size();
    }

    private void getViewablesIfNecessary() throws MessagingException {
        if (viewables == null) {
            attachments = new ArrayList<Part>();
            viewables = MessageExtractor.getViewables(message, attachments);
        }
    }
}
