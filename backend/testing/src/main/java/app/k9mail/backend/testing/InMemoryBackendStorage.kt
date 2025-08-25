package app.k9mail.backend.testing

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

class InMemoryBackendStorage : BackendStorage {
    val folders: MutableMap<String, InMemoryBackendFolder> = mutableMapOf()
    val extraStrings: MutableMap<String, String> = mutableMapOf()
    val extraNumbers: MutableMap<String, Long> = mutableMapOf()

    override fun getFolder(folderServerId: String): InMemoryBackendFolder {
        return folders[folderServerId] ?: error("Folder $folderServerId not found")
    }

    override fun getFolderServerIds(): List<String> {
        return folders.keys.toList()
    }

    override fun createFolderUpdater(): BackendFolderUpdater {
        return InMemoryBackendFolderUpdater()
    }

    override fun getExtraString(name: String): String? = extraStrings[name]

    override fun setExtraString(name: String, value: String) {
        extraStrings[name] = value
    }

    override fun getExtraNumber(name: String): Long? = extraNumbers[name]

    override fun setExtraNumber(name: String, value: Long) {
        extraNumbers[name] = value
    }

    private inner class InMemoryBackendFolderUpdater : BackendFolderUpdater {
        override fun createFolders(folders: List<FolderInfo>): Set<Long> {
            var count = this@InMemoryBackendStorage.folders.size.toLong()
            return buildSet {
                folders.forEach { folder ->
                    if (this@InMemoryBackendStorage.folders.containsKey(folder.serverId)) {
                        error("Folder ${folder.serverId} already present")
                    }

                    this@InMemoryBackendStorage.folders[folder.serverId] = InMemoryBackendFolder(
                        name = folder.name,
                        type = folder.type,
                    )
                    add(count++)
                }
            }
        }

        override fun deleteFolders(folderServerIds: List<String>) {
            for (folderServerId in folderServerIds) {
                folders.remove(folderServerId) ?: error("Folder $folderServerId not found")
            }
        }

        override fun changeFolder(folderServerId: String, name: String, type: FolderType) {
            val folder = folders[folderServerId] ?: error("Folder $folderServerId not found")
            folder.name = name
            folder.type = type
        }

        override fun close() = Unit
    }
}
