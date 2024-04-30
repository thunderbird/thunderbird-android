package com.fsck.k9.backend.imap

import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandExpunge(private val imapStore: ImapStore) {

    fun expunge(folderServerId: String) {
        Timber.d("processPendingExpunge: folder = %s", folderServerId)

        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.expunge()

            Timber.d("processPendingExpunge: complete for folder = %s", folderServerId)
        } finally {
            remoteFolder.close()
        }
    }

    fun expungeMessages(folderServerId: String, messageServerIds: List<String>) {
        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.expungeUids(messageServerIds)
        } finally {
            remoteFolder.close()
        }
    }
}
