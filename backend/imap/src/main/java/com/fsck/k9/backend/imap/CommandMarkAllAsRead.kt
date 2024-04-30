package com.fsck.k9.backend.imap

import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandMarkAllAsRead(private val imapStore: ImapStore) {

    fun markAllAsRead(folderServerId: String) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)
            if (remoteFolder.mode != OpenMode.READ_WRITE) return

            remoteFolder.setFlagsForAllMessages(setOf(Flag.SEEN), true)
        } finally {
            remoteFolder.close()
        }
    }
}
