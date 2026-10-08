package net.thunderbird.feature.mail.message.list.internal.fakes

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType

internal open class FakeBackendFolderUpdater(
    private val exception: Exception? = null,
    private val returnEmptySetWhenCreatingFolders: Boolean = false,
) : BackendFolderUpdater {
    private val ids = mutableSetOf<Long>()
    val createFoldersCalls = mutableListOf<List<FolderInfo>>()
    val changeFolderCalls = mutableListOf<ChangeFolderCall>()
    var closeCalls = 0

    override fun createFolders(folders: List<FolderInfo>): Set<Long> {
        createFoldersCalls += folders
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
        changeFolderCalls += ChangeFolderCall(folderServerId, name, type)
        if (exception != null) throw exception
    }

    override fun close() {
        closeCalls += 1
        if (exception != null) throw exception
    }

    data class ChangeFolderCall(
        val folderServerId: String,
        val name: String,
        val type: FolderType,
    )
}
