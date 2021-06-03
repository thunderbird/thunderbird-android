package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandUploadMessage(private val imapStore: ImapStore) {

    fun uploadMessage(folderServerId: String, message: Message): String? {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(OpenMode.READ_WRITE)

            val localUid = message.uid
            val uidMap = folder.appendMessages(listOf(message))

            return uidMap?.get(localUid)
        } finally {
            folder.close()
        }
    }
}
