package app.k9mail.backend.demo

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.Message

internal class DemoStore(
    private val demoDataLoader: DemoDataLoader = DemoDataLoader(),
) {
    private val demoFolders: DemoFolders by lazy { demoDataLoader.loadFolders() }

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
}
