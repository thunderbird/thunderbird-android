package com.fsck.k9.backend.pop3

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.pop3.Pop3Store

internal class CommandSetFlag(private val pop3Store: Pop3Store) {

    @Throws(MessagingException::class)
    fun setFlag(
        folderServerId: String,
        messageServerIds: List<String>,
        flag: Flag,
        newState: Boolean,
    ) {
        val folder = pop3Store.getFolder(folderServerId)
        if (!folder.isFlagSupported(flag)) {
            return
        }

        try {
            folder.open()

            val messages = messageServerIds.map { folder.getMessage(it) }
            if (messages.isEmpty()) {
                return
            }

            folder.setFlags(messages, setOf(flag), newState)
        } finally {
            folder.close()
        }
    }
}
