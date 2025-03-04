package app.k9mail.backend.demo

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message

internal class DemoStore(
    private val demoDataLoader: DemoDataLoader = DemoDataLoader(),
) {
    private val demoFolders: DemoFolders by lazy { flattenDemoFolders(demoDataLoader.loadFolders()) }

    fun getFolder(folderServerId: String): DemoFolder? {
        return demoFolders[folderServerId]
    }

    fun getFolderIds(): Set<String> {
        return demoFolders.keys
    }

    fun getInboxFolderId(): String {
        return demoFolders.filterValues { it.type == FolderType.INBOX }.keys.first()
    }

    fun getMessage(folderServerId: String, messageServerId: String): Message {
        return demoDataLoader.loadMessage(folderServerId, messageServerId)
    }

    // This is a workaround for the fact that the backend doesn't support nested folders
    private fun flattenDemoFolders(
        demoFolders: DemoFolders,
        parentName: String = "",
        parentServerId: String = "",
    ): DemoFolders {
        val flatFolders = mutableMapOf<String, DemoFolder>()
        for ((folderServerId, demoFolder) in demoFolders) {
            val fullName = if (parentName.isEmpty()) {
                demoFolder.name
            } else {
                "$parentName/${demoFolder.name}"
            }
            val fullServerId = if (parentServerId.isEmpty()) {
                folderServerId
            } else {
                "$parentServerId/$folderServerId"
            }
            flatFolders[fullServerId] = demoFolder.copy(name = fullName)

            val subFolders = demoFolder.subFolders
            if (subFolders != null) {
                flatFolders.putAll(flattenDemoFolders(demoFolder.subFolders, fullName, fullServerId))
            }
        }
        return flatFolders
    }
}
