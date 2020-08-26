package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandSetFlag(private val imapStore: ImapStore) {

    fun setFlag(folderServerId: String, messageServerIds: List<String>, flag: Flag, newState: Boolean) {
        if (messageServerIds.isEmpty()) return

        val remoteFolder = imapStore.getFolder(folderServerId)
        if (!remoteFolder.exists()) return

        try {
            remoteFolder.open(ImapFolder.OPEN_MODE_RW)
            if (remoteFolder.mode != ImapFolder.OPEN_MODE_RW) return

            val messages = messageServerIds.map { uid -> remoteFolder.getMessage(uid) }

            remoteFolder.setFlags(messages, setOf(flag), newState)
        } finally {
            remoteFolder.close()
        }
    }
}
