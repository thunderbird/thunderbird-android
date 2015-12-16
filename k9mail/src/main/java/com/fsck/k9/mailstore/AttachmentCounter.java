package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;


class AttachmentCounter {
    public int getAttachmentCount(Message message) throws MessagingException {
        List<Part> attachments = new ArrayList<Part>();
        MessageExtractor.getViewables(message, attachments);

        return attachments.size();
    }
}
