package com.fsck.k9.backend.webdav


import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.store.webdav.WebDavStore


internal class CommandGetFolders(private val webDavStore: WebDavStore) {
    fun getFolders(forceListAll: Boolean): List<FolderInfo> {
        return webDavStore.getPersonalNamespaces(forceListAll).map {
            FolderInfo(it.serverId, it.name)
        }
    }
}
