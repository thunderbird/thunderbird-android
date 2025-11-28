package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.core.common.mail.Flag

internal class CommandMarkAllAsRead(private val imapStore: ImapStore) {

    fun markAllAsRead(folderServerId: String) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.setFlagsForAllMessages(setOf(Flag.SEEN), true)
        } finally {
            remoteFolder.close()
        }
    }
}
