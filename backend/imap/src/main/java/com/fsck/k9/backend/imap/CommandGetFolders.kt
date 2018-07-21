package com.fsck.k9.backend.imap


import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.store.imap.ImapStore


internal class CommandGetFolders(private val imapStore: ImapStore) {
    fun getFolders(): List<FolderInfo> {
        return imapStore.personalNamespaces.map {
            FolderInfo(it.serverId, it.name)
        }
    }
}
