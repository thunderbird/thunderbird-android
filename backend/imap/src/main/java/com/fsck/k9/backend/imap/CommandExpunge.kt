package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore
import timber.log.Timber

internal class CommandExpunge(private val imapStore: ImapStore) {

    fun expunge(folderServerId: String) {
        Timber.d("processPendingExpunge: folder = %s", folderServerId)

        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            if (!remoteFolder.exists()) return

            remoteFolder.open(ImapFolder.OPEN_MODE_RW)
            if (remoteFolder.mode != ImapFolder.OPEN_MODE_RW) return

            remoteFolder.expunge()

            Timber.d("processPendingExpunge: complete for folder = %s", folderServerId)
        } finally {
            remoteFolder.close()
        }
    }

    fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            if (!remoteFolder.exists()) return

            remoteFolder.open(ImapFolder.OPEN_MODE_RW)
            if (remoteFolder.mode != ImapFolder.OPEN_MODE_RW) return

            remoteFolder.expungeUids(messageServerIds)
        } finally {
            remoteFolder.close()
        }
    }
}
