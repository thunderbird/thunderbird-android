package com.fsck.k9.helper;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.ListHeaders;
import com.fsck.k9.mail.internet.MimeMessage;

public class ReplyToParser {
    public static Address[] getRecipientsToReplyTo(Message message) {
        Address[] replyToAddresses;
        if (message.getReplyTo().length > 0)
            return message.getReplyTo();
        if (message instanceof MimeMessage) {
            Address[] listPostAddresses = ListHeaders.getListPostAddresses((MimeMessage) message);
            if (listPostAddresses.length > 0) {
                return listPostAddresses;
            }
        }
        return message.getFrom();
    }
}
