package net.thunderbird.app.common.feature.mail

import com.fsck.k9.backend.api.BackendFolderUpdater
import com.fsck.k9.backend.api.BackendStorage
import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mailstore.LegacyAccountDtoBackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountDto

internal class FakeLegacyAccountDtoBackendStorageFactory : LegacyAccountDtoBackendStorageFactory {
    var lastAccount: LegacyAccountDto? = null

    override fun createBackendStorage(account: LegacyAccountDto): BackendStorage {
        lastAccount = account
        return object : BackendStorage {
            override fun getFolder(folderServerId: String) = throw UnsupportedOperationException()
            override fun getFolderServerIds(): List<String> = emptyList()
            override fun createFolderUpdater(): BackendFolderUpdater = object : BackendFolderUpdater {
                override fun createFolders(folders: List<FolderInfo>): Set<Long> = emptySet()
                override fun deleteFolders(folderServerIds: List<String>) = Unit
                override fun changeFolder(
                    folderServerId: String,
                    name: String,
                    type: FolderType,
                ) = Unit
                override fun close() = Unit
            }
            override fun getExtraString(name: String): String? = null
            override fun setExtraString(name: String, value: String) = Unit
            override fun getExtraNumber(name: String): Long? = null
            override fun setExtraNumber(name: String, value: Long) = Unit
        }
    }
}
