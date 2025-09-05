package app.k9mail.backend.demo

import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.updateFolders
import net.thunderbird.feature.mail.folder.api.FOLDER_DEFAULT_PATH_DELIMITER
import net.thunderbird.feature.mail.folder.api.FolderPathDelimiter

internal class CommandRefreshFolderList(
    private val backendStorage: BackendStorage,
    private val demoStore: DemoStore,
) {

    fun refreshFolderList(): FolderPathDelimiter? {
        val localFolderServerIds = backendStorage.getFolderServerIds().toSet()

        backendStorage.updateFolders {
            val remoteFolderServerIds = demoStore.getFolderIds()
            val foldersServerIdsToCreate = remoteFolderServerIds - localFolderServerIds
            val foldersToCreate = foldersServerIdsToCreate.mapNotNull { folderServerId ->
                demoStore.getFolder(folderServerId)?.let { folderData ->
                    FolderInfo(folderServerId, folderData.name, folderData.type)
                }
            }
            createFolders(foldersToCreate)

            val folderServerIdsToRemove = (localFolderServerIds - remoteFolderServerIds).toList()
            deleteFolders(folderServerIdsToRemove)
        }

        return FOLDER_DEFAULT_PATH_DELIMITER
    }
}
