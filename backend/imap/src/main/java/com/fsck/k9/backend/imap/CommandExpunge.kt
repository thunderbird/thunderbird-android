package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.legacy.logging.Log

private const val TAG = "CommandExpunge"

internal class CommandExpunge(private val imapStore: ImapStore) {

    fun expunge(folderServerId: String) {
        Log.d(TAG, "processPendingExpunge: folder = %s", folderServerId)

        val remoteFolder = imapStore.getFolder(folderServerId)
        try {
            remoteFolder.open(OpenMode.READ_WRITE)

            remoteFolder.expunge()

            Log.d(TAG, "processPendingExpunge: complete for folder = %s", folderServerId)
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
