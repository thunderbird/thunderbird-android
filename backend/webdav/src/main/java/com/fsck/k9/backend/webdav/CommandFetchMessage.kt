package com.fsck.k9.backend.webdav

import com.fsck.k9.mail.FetchProfile
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.store.webdav.WebDavStore

internal class CommandFetchMessage(private val webDavStore: WebDavStore) {

    fun fetchMessage(folderServerId: String, messageServerId: String, fetchProfile: FetchProfile): Message {
        val folder = webDavStore.getFolder(folderServerId)
        try {
            val message = folder.getMessage(messageServerId)

            folder.fetch(listOf(message), fetchProfile, null)

            return message
        } finally {
            folder.close()
        }
    }
}
