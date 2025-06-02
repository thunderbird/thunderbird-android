package net.thunderbird.feature.mail.message.list.fakes

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

internal open class FakeBackendFolderUpdater(
    private val exception: Exception? = null,
) : BackendFolderUpdater {
    private val ids = mutableSetOf<Long>()
    override fun createFolders(folders: List<FolderInfo>): Set<Long> {
        if (exception != null) throw exception

        var last = ids.last()

        ids.addAll(folders.map { ++last })

        return ids
    }

    override fun deleteFolders(folderServerIds: List<String>) {
        if (exception != null) throw exception
    }

    override fun changeFolder(folderServerId: String, name: String, type: FolderType) {
        if (exception != null) throw exception
    }

    override fun close() {
        if (exception != null) throw exception
    }
}
