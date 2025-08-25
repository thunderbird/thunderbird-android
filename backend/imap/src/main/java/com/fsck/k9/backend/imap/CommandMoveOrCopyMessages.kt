package com.fsck.k9.backend.imap

import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode
import net.thunderbird.core.logging.legacy.Log

internal class CommandMoveOrCopyMessages(private val imapStore: ImapStore) {

    fun moveMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        return moveOrCopyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds, false)
    }

    fun copyMessages(
        sourceFolderServerId: String,
        targetFolderServerId: String,
        messageServerIds: List<String>,
    ): Map<String, String>? {
        return moveOrCopyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds, true)
    }

    private fun moveOrCopyMessages(
        srcFolder: String,
        destFolder: String,
        uids: Collection<String>,
        isCopy: Boolean,
    ): Map<String, String>? {
        var remoteSrcFolder: ImapFolder? = null
        var remoteDestFolder: ImapFolder? = null

        return try {
            remoteSrcFolder = imapStore.getFolder(srcFolder)

            if (uids.isEmpty()) {
                Log.i("moveOrCopyMessages: no remote messages to move, skipping")
                return null
            }

            remoteSrcFolder.open(OpenMode.READ_WRITE)

            val messages = uids.map { uid -> remoteSrcFolder.getMessage(uid) }

            Log.d(
                "moveOrCopyMessages: source folder = %s, %d messages, destination folder = %s, isCopy = %s",
                srcFolder,
                messages.size,
                destFolder,
                isCopy,
            )

            remoteDestFolder = imapStore.getFolder(destFolder)
            if (isCopy) {
                remoteSrcFolder.copyMessages(messages, remoteDestFolder)
            } else {
                remoteSrcFolder.moveMessages(messages, remoteDestFolder)
            }
        } finally {
            remoteSrcFolder?.close()
            remoteDestFolder?.close()
        }
    }
}
