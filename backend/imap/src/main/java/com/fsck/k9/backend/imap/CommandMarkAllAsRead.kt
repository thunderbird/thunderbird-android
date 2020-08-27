package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandMarkAllAsRead(private val imapStore: ImapStore) {

    fun markAllAsRead(folderServerId: String) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        if (!remoteFolder.exists()) return

        try {
            remoteFolder.open(ImapFolder.OPEN_MODE_RW)
            if (remoteFolder.mode != ImapFolder.OPEN_MODE_RW) return

            remoteFolder.setFlags(setOf(Flag.SEEN), true)
        } finally {
            remoteFolder.close()
        }
    }
}
