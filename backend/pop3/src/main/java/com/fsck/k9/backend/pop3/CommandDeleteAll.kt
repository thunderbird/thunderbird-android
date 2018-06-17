package com.fsck.k9.backend.pop3


import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Folder
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.store.pop3.Pop3Store


internal class CommandDeleteAll(private val pop3Store: Pop3Store) {

    @Throws(MessagingException::class)
    fun deleteAll(folderServerId: String) {
        val remoteFolder = pop3Store.getFolder(folderServerId)
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
