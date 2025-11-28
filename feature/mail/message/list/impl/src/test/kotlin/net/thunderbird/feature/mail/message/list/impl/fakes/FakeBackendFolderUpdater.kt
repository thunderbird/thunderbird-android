package net.thunderbird.feature.mail.message.list.impl.fakes

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

internal open class FakeBackendFolderUpdater(
    private val exception: Exception? = null,
    private val returnEmptySetWhenCreatingFolders: Boolean = false,
) : BackendFolderUpdater {
    private val ids = mutableSetOf<Long>()
    override fun createFolders(folders: List<FolderInfo>): Set<Long> {
        return when {
            exception != null -> throw exception
            returnEmptySetWhenCreatingFolders -> emptySet()
            else -> ids.apply {
                var last = ids.lastOrNull() ?: 0
                addAll(folders.map { ++last })
            }
        }
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
