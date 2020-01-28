package com.fsck.k9.backend.webdav

import com.fsck.k9.mail.Message
import com.fsck.k9.mail.store.webdav.WebDavStore

internal class CommandUploadMessage(private val webDavStore: WebDavStore) {

    fun uploadMessage(folderServerId: String, message: Message): String? {
        val folder = webDavStore.getFolder(folderServerId)
        try {
            folder.open()

            folder.appendMessages(listOf(message))

            return null
        } finally {
            folder.close()
        }
    }
}
