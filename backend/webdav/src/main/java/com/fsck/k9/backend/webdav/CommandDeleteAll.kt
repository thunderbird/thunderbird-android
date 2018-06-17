package com.fsck.k9.backend.webdav


import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.webdav.WebDavStore


internal class CommandDeleteAll(private val webDavStore: WebDavStore) {

    @Throws(MessagingException::class)
    fun deleteAll(folderServerId: String) {
        val remoteFolder = webDavStore.getFolder(folderServerId)
        if (!remoteFolder.exists()) {
            return
        }

        try {
            remoteFolder.open(Folder.OPEN_MODE_RW)
            remoteFolder.setFlags(setOf(Flag.DELETED), true)
        } finally {
            remoteFolder.close()
        }
    }
}
