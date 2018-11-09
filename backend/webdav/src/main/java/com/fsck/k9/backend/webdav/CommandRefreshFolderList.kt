package com.fsck.k9.backend.webdav


import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.Folder.FolderType
import com.fsck.k9.mail.store.webdav.WebDavStore


internal class CommandRefreshFolderList(
        private val backendStorage: BackendStorage,
        private val webDavStore: WebDavStore
) {
    fun refreshFolderList() {
        val foldersOnServer = webDavStore.personalNamespaces
        val oldFolderServerIds = backendStorage.getFolderServerIds()

        val foldersToCreate = mutableListOf<FolderInfo>()
        for (folder in foldersOnServer) {
            //FIXME: Use correct folder type
            if (folder.serverId !in oldFolderServerIds) {
                foldersToCreate.add(FolderInfo(folder.serverId, folder.name, FolderType.REGULAR))
            } else {
                backendStorage.changeFolder(folder.serverId, folder.name, FolderType.REGULAR)
            }
        }
        backendStorage.createFolders(foldersToCreate)

        val newFolderServerIds = foldersOnServer.map { it.serverId }
        val removedFolderServerIds = oldFolderServerIds - newFolderServerIds
        backendStorage.deleteFolders(removedFolderServerIds)
    }
}
