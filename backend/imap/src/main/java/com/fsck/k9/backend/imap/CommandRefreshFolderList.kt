package com.fsck.k9.backend.imap

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.store.imap.FolderListItem
import com.fsck.k9.mail.store.imap.ImapStore
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

private const val TAG = "CommandRefreshFolderList"

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val imapStore: ImapStore,
    private val logger: Logger = Log,
) {

    private val LegacyFolderListItem.normalizedServerId: String
        get() = imapStore.combinedPrefix?.let {
            serverId.removePrefix(prefix = it)
        } ?: serverId

    fun refreshFolderList(): FolderPathDelimiter? {
        logger.verbose(TAG) { "refreshFolderList() called" }
        val folders = imapStore.getFolders()
        val folderPathDelimiter = folders.firstOrNull { it.folderPathDelimiter != null }?.folderPathDelimiter
        val foldersOnServer = folders.toLegacyFolderList()
        val oldFolderServerIds = backendStorage.getFolderServerIds()

        backendStorage.updateFolders {
            val foldersToCreate = mutableListOf<FolderInfo>()
            for (folder in foldersOnServer) {
                if (folder.normalizedServerId !in oldFolderServerIds) {
                    foldersToCreate.add(FolderInfo(folder.normalizedServerId, folder.name, folder.type))
                } else {
                    changeFolder(folder.normalizedServerId, folder.name, folder.type)
                }
            }

            logger.verbose(TAG) { "refreshFolderList: foldersToCreate = $foldersToCreate" }
            createFolders(foldersToCreate)

            val newFolderServerIds = foldersOnServer.map { it.normalizedServerId }
            val removedFolderServerIds = oldFolderServerIds - newFolderServerIds
            logger.verbose(TAG) { "refreshFolderList: folders to remove = $removedFolderServerIds" }
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
