package com.fsck.k9.backend.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandFetchMessage(private val imapStore: ImapStore) {

    fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(ImapFolder.OPEN_MODE_RW)

            val message = folder.getMessage(messageServerId)
            folder.fetchPart(message, part, null, bodyFactory, -1)
        } finally {
            folder.close()
        }
    }
}
