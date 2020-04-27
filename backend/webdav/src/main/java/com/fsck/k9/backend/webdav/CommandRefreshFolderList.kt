package com.fsck.k9.backend.webdav

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.store.webdav.WebDavStore

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val webDavStore: WebDavStore
) {
    fun refreshFolderList() {
        val foldersOnServer = webDavStore.personalNamespaces
        val oldFolderServerIds = backendStorage.getFolderServerIds()

        backendStorage.updateFolders {
            val foldersToCreate = mutableListOf<FolderInfo>()
            for (folder in foldersOnServer) {
                if (folder.serverId !in oldFolderServerIds) {
                    foldersToCreate.add(FolderInfo(folder.serverId, folder.name, folder.type))
                } else {
                    changeFolder(folder.serverId, folder.name, folder.type)
                }
            }
            createFolders(foldersToCreate)

            val newFolderServerIds = foldersOnServer.map { it.serverId }
            val removedFolderServerIds = oldFolderServerIds - newFolderServerIds
            deleteFolders(removedFolderServerIds)
        }
    }
}
