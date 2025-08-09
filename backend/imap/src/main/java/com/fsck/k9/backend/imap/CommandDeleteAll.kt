package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.core.common.exception.MessagingException

internal class CommandDeleteAll(private val imapStore: ImapStore) {

    @Throws(MessagingException::class)
    fun deleteAll(folderServerId: String) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.deleteAllMessages()
        } finally {
            remoteFolder.close()
        }
    }
}
