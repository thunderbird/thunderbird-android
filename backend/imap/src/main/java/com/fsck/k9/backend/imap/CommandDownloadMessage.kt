package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.FetchProfile.Item.BODY
import com.fsck.k9.mail.FetchProfile.Item.ENVELOPE
import com.fsck.k9.mail.FetchProfile.Item.FLAGS
import com.fsck.k9.mail.FetchProfile.Item.STRUCTURE
import com.fsck.k9.mail.MessageDownloadState
import com.fsck.k9.mail.helper.fetchProfileOf
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.fsck.k9.mail.store.imap.ImapStore
import com.fsck.k9.mail.store.imap.OpenMode

internal class CommandDownloadMessage(private val backendStorage: BackendStorage, private val imapStore: ImapStore) {

    fun downloadMessageStructure(folderServerId: String, messageServerId: String) {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(OpenMode.READ_ONLY)

            val message = folder.getMessage(messageServerId)

            // fun fact: ImapFolder.fetch can't handle getting STRUCTURE at same time as headers
            fetchMessage(folder, message, fetchProfileOf(FLAGS, ENVELOPE))
            fetchMessage(folder, message, fetchProfileOf(STRUCTURE))

            val backendFolder = backendStorage.getFolder(folderServerId)
            backendFolder.saveMessage(message, MessageDownloadState.ENVELOPE)
        } finally {
            folder.close()
        }
    }

    fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(OpenMode.READ_ONLY)

            val message = folder.getMessage(messageServerId)
            fetchMessage(folder, message, fetchProfileOf(FLAGS, BODY))

            val backendFolder = backendStorage.getFolder(folderServerId)
            backendFolder.saveMessage(message, MessageDownloadState.FULL)
        } finally {
            folder.close()
        }
    }

    private fun fetchMessage(remoteFolder: ImapFolder, message: ImapMessage, fetchProfile: FetchProfile) {
        val maxDownloadSize = 0
        remoteFolder.fetch(listOf(message), fetchProfile, null, maxDownloadSize)
    }
}
