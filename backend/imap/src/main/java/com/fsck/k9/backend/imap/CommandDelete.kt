package com.fsck.k9.backend.imap

import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandDelete(private val imapStore: ImapStore) {

    @Throws(MessagingException::class)
    fun deleteMessages(folderServerId: String, messageServerIds: List<String>) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            val messages = messageServerIds.map { uid -> remoteFolder.getMessage(uid) }

            remoteFolder.deleteMessages(messages)
        } finally {
            remoteFolder.close()
        }
    }
}
