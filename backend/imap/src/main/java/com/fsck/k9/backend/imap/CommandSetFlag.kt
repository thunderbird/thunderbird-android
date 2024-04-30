package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandSetFlag(private val imapStore: ImapStore) {

    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        if (messageServerIds.isEmpty()) return

        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            val messages = messageServerIds.map { uid -> remoteFolder.getMessage(uid) }

            remoteFolder.setFlags(messages, setOf(flag), newState)
        } finally {
            remoteFolder.close()
        }
    }
}
