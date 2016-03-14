package com.fsck.k9.helper;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;

public class ReplyToParser {
    public static Address[] getRecipientsToReplyTo(Message message) {
        Address[] replyToAddresses;
        if (message.getReplyTo().length > 0) {
            replyToAddresses = message.getReplyTo();
        } else if (message.getListPost().length > 0) {
            replyToAddresses = message.getListPost();
        } else {
            replyToAddresses = message.getFrom();
        }
        return replyToAddresses;
    }
}
