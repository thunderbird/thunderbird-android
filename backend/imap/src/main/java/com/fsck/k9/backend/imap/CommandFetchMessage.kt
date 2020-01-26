package com.fsck.k9.backend.imap

import com.fsck.k9.mail.BodyFactory
import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.store.imap.ImapFolder
import com.fsck.k9.mail.store.imap.ImapMessage
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandFetchMessage(private val imapStore: ImapStore) {

    fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(ImapFolder.OPEN_MODE_RO)

            val message = folder.getMessage(messageServerId)

            // fun fact: ImapFolder.fetch can't handle getting STRUCTURE at same time as headers
            if (fetchProfile.contains(FetchProfile.Item.STRUCTURE) &&
                    fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
                val headerFetchProfile = fetchProfile.without(FetchProfile.Item.STRUCTURE)
                val structureFetchProfile = FetchProfile().apply { add(FetchProfile.Item.STRUCTURE) }

                fetchMessage(folder, message, headerFetchProfile)
                fetchMessage(folder, message, structureFetchProfile)
            } else {
                fetchMessage(folder, message, fetchProfile)
            }

            return message
        } finally {
            folder.close()
        }
    }

    fun fetchPart(folderServerId: String, messageServerId: String, part: Part, bodyFactory: BodyFactory) {
        val folder = imapStore.getFolder(folderServerId)
        try {
            folder.open(ImapFolder.OPEN_MODE_RW)

            val message = folder.getMessage(messageServerId)
            folder.fetchPart(message, part, null, bodyFactory)
        } finally {
            folder.close()
        }
    }

    private fun fetchMessage(remoteFolder: ImapFolder, message: ImapMessage, fetchProfile: FetchProfile) {
        remoteFolder.fetch(listOf(message), fetchProfile, null)
    }

    private fun FetchProfile.without(item: FetchProfile.Item) = FetchProfile().apply {
        this@without.forEach {
            if (it != item) add(it)
        }
    }
}
