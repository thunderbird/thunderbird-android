package com.fsck.k9.controller;


import java.util.Comparator;

import com.fsck.k9.mail.Message;


class UidReverseComparator implements Comparator<Message> {
    @Override
    public int compare(Message messageLeft, Message messageRight) {
        Integer uidLeft = getUidForMessage(messageLeft);
        Integer uidRight = getUidForMessage(messageRight);

        if (uidLeft == null && uidRight == null) {
            return 0;
        } else if (uidLeft == null) {
            return 1;
        } else if (uidRight == null) {
            return -1;
        }

        // reverse order
        return uidRight.compareTo(uidLeft);
    }

    private Integer getUidForMessage(Message message) {
        try {
            return Integer.parseInt(message.getUid());
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }
}
