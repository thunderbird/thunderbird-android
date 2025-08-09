package com.fsck.k9.message.extractors;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Message;
import net.thunderbird.core.common.exception.MessagingException;
import com.fsck.k9.mail.Part;
import com.fsck.k9.mail.internet.MessageExtractor;


public class AttachmentCounter {

    public static AttachmentCounter newInstance() {
        return new AttachmentCounter();
    }

    public int getAttachmentCount(Message message) throws MessagingException {
        List<Part> attachmentParts = new ArrayList<>();
        MessageExtractor.findViewablesAndAttachments(message, null, attachmentParts);

        return attachmentParts.size();
    }
}
