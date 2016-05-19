package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;
import com.fsck.k9.mail.internet.Viewable;


class AttachmentCounter {
    public int getAttachmentCount(Message message) throws MessagingException {
        List<Part> attachments = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, new ArrayList<Viewable>(), attachments);

        return attachments.size();
    }
}
