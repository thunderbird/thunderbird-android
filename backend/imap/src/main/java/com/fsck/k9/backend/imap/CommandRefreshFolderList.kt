package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val imapStore: ImapStore
) {
    fun refreshFolderList() {
        val foldersOnServer = imapStore.folders
        val oldFolderServerIds = backendStorage.getFolderServerIds()

        backendStorage.updateFolders {
            val foldersToCreate = mutableListOf<FolderInfo>()
            for (folder in foldersOnServer) {
                // TODO: Start using the proper server ID. For now we still use the old server ID.
                val serverId = folder.oldServerId ?: continue

                if (serverId !in oldFolderServerIds) {
                    foldersToCreate.add(FolderInfo(serverId, folder.name, folder.type))
                } else {
                    changeFolder(serverId, folder.name, folder.type)
                }
            }
            createFolders(foldersToCreate)

            val newFolderServerIds = foldersOnServer.map { it.serverId }
            val removedFolderServerIds = oldFolderServerIds - newFolderServerIds
            deleteFolders(removedFolderServerIds)
        }
    }
}
