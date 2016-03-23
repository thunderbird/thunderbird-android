package com.fsck.k9.helper;


import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.internet.ListHeaders;


public class ReplyToParser {
    public static Address[] getRecipientsToReplyTo(Message message) {
        Address[] replyToAddresses = message.getReplyTo();
        if (replyToAddresses.length > 0) {
            return replyToAddresses;
        }

        Address[] listPostAddresses = ListHeaders.getListPostAddresses(message);
        if (listPostAddresses.length > 0) {
            return listPostAddresses;
        }

        return message.getFrom();
    }
}
