package com.fsck.k9.backend.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandFetchMessage(private val imapStore: ImapStore) {

    fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(OpenMode.READ_WRITE)

            val message = folder.getMessage(messageServerId)
            folder.fetchPart(message, part, bodyFactory, -1)
        } finally {
            folder.close()
        }
    }
}
