package com.fsck.k9.backend.webdav

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.mail.FetchProfile.Item.BODY
import com.fsck.k9.mail.FetchProfile.Item.FLAGS
import com.fsck.k9.mail.helper.fetchProfileOf
import com.fsck.k9.mail.store.webdav.WebDavStore

internal class CommandDownloadMessage(val backendStorage: BackendStorage, private val webDavStore: WebDavStore) {

    fun downloadCompleteMessage(folderServerId: String, messageServerId: String) {
        val folder = webDavStore.getFolder(folderServerId)
        try {
            val message = folder.getMessage(messageServerId)

            folder.fetch(listOf(message), fetchProfileOf(FLAGS, BODY), null, 0)

            val backendFolder = backendStorage.getFolder(folderServerId)
            backendFolder.saveCompleteMessage(message)
        } finally {
            folder.close()
        }
    }
}
