package com.fsck.k9.backend.pop3

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.pop3.Pop3Message
import com.fsck.k9.mail.store.pop3.Pop3Store
import java.util.ArrayList

internal class CommandSetFlag(pop3Store: Pop3Store) {
    private val pop3Store: Pop3Store

    init {
        this.pop3Store = pop3Store
    }

    @Throws(MessagingException::class)
    fun setFlag(
        folderServerId: String, messageServerIds: MutableList<String?>, flag: Flag,
        newState: Boolean
    ) {
        val remoteFolder = pop3Store.getFolder(folderServerId)
        if (!remoteFolder.isFlagSupported(flag)) {
            return
        }

        try {
            remoteFolder.open()
            val messages: MutableList<Pop3Message?> = ArrayList<Pop3Message?>()
            for (uid in messageServerIds) {
                messages.add(remoteFolder.getMessage(uid))
            }

            if (messages.isEmpty()) {
                return
            }
            remoteFolder.setFlags(messages, mutableSetOf<Flag?>(flag), newState)
        } finally {
            remoteFolder.close()
        }
    }
}
