package com.fsck.k9.helper;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.support.annotation.VisibleForTesting;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.internet.ListHeaders;


public class ReplyToParser {

    public Address[] getRecipientsToReplyTo(Message message, Account account) {
        Address[] result = message.getReplyTo();

        boolean hasNoReplyToAddresses = (result.length == 0);
        if (hasNoReplyToAddresses) {
            result = ListHeaders.getListPostAddresses(message);
        }

        boolean hasNoListPostAddresses = result.length == 0;
        if (hasNoListPostAddresses) {
            result = message.getFrom();
        }

        boolean replyToAddressIsUserIdentity = account.isAnIdentity(result);
        if (replyToAddressIsUserIdentity) {
            result = message.getRecipients(RecipientType.TO);
        }

        return result;
    }

    public ReplyToAddresses getRecipientsToReplyAllTo(
            Message message, Address[] replyToAddresses, Account account) {
        HashSet<Address> alreadyAddedAddresses = new HashSet<>(Arrays.asList(replyToAddresses));
        ArrayList<Address> toAddresses = new ArrayList<>();
        ArrayList<Address> ccAddresses = new ArrayList<>();

        for (Address address : message.getFrom()) {
            if (!alreadyAddedAddresses.contains(address) && !account.isAnIdentity(address)) {
                toAddresses.add(address);
                alreadyAddedAddresses.add(address);
            }
        }

        for (Address address : message.getRecipients(RecipientType.TO)) {
            if (!alreadyAddedAddresses.contains(address) && !account.isAnIdentity(address)) {
                toAddresses.add(address);
                alreadyAddedAddresses.add(address);
            }
        }

        for (Address address : message.getRecipients(RecipientType.CC)) {
            if (!alreadyAddedAddresses.contains(address) && !account.isAnIdentity(address)) {
                ccAddresses.add(address);
                alreadyAddedAddresses.add(address);
            }
        }

        return new ReplyToAddresses(toAddresses, ccAddresses);
    }

    public static class ReplyToAddresses {
        public final Address[] to;
        public final Address[] cc;

        @VisibleForTesting
        public ReplyToAddresses(List<Address> toAddresses, List<Address> ccAddresses) {
            to = toAddresses.toArray(new Address[toAddresses.size()]);
            cc = ccAddresses.toArray(new Address[ccAddresses.size()]);
        }
    }

}
