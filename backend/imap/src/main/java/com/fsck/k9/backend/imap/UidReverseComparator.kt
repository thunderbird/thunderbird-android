package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Message
import java.util.Comparator

internal class UidReverseComparator : Comparator<Message> {
    override fun compare(messageLeft: Message, messageRight: Message): Int {
        val uidLeft = messageLeft.uidOrNull
        val uidRight = messageRight.uidOrNull
        if (uidLeft == null && uidRight == null) {
            return 0
        } else if (uidLeft == null) {
            return 1
        } else if (uidRight == null) {
            return -1
        }

        // reverse order
        return uidRight.compareTo(uidLeft)
    }

    private val Message.uidOrNull
        get() = uid?.toLongOrNull()
}
