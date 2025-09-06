package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.store.imap.FolderListItem
import com.fsck.k9.mail.store.imap.ImapStore
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val imapStore: ImapStore,
) {
    fun refreshFolderList(): FolderPathDelimiter? {
        val folders = imapStore.getFolders()
        val folderPathDelimiter = folders.firstOrNull { it.folderPathDelimiter != null }?.folderPathDelimiter
        val foldersOnServer = folders.toLegacyFolderList()
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
        return folderPathDelimiter
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
