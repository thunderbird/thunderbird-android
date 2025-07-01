package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.store.imap.FolderListItem
import com.fsck.k9.mail.store.imap.ImapStore

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val imapStore: ImapStore,
) {
    fun refreshFolderList() {
        // TODO: Start using the proper server ID.
        //  For now we still use the old server ID format (decoded, with prefix removed).
        val foldersOnServer = imapStore.getFolders().toLegacyFolderList()
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

private fun List<FolderListItem>.toLegacyFolderList(): List<LegacyFolderListItem> {
    return this
        .map { LegacyFolderListItem(it.serverId, it.name, it.type) }
}

private data class LegacyFolderListItem(
    val serverId: String,
    val name: String,
    val type: FolderType,
)
