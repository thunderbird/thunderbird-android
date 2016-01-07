package com.fsck.k9.controller;


import java.util.Comparator;

import com.fsck.k9.mail.Message;


class UidReverseComparator implements Comparator<Message> {
    @Override
    public int compare(Message messageLeft, Message messageRight) {
        Integer uidLeft, uidRight;
        try {
            uidLeft = Integer.parseInt(messageLeft.getUid());
        } catch (NullPointerException | NumberFormatException e) {
            uidLeft = null;
        }
        try {
            uidRight = Integer.parseInt(messageRight.getUid());
        } catch (NullPointerException | NumberFormatException e) {
            uidRight = null;
        }

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
}
