package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.createFolder
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mailstore.LegacyAccountDtoBackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log

internal class BackendStorageArchiveFolderCreator(
    private val backendStorageFactory: LegacyAccountDtoBackendStorageFactory,
) : ArchiveFolderCreator {
    @Suppress("TooGenericExceptionCaught")
    override fun createFolder(account: LegacyAccountDto, folderInfo: FolderInfo): Long? {
        return try {
            val backendStorage = backendStorageFactory.createBackendStorage(account)
            backendStorage.updateFolders {
                createFolder(folderInfo)
            }
        } catch (e: Exception) {
            Log.e(e, "Failed to create archive subfolder: ${folderInfo.serverId}")
            // TODO: Inform the user that archive folder creation failed. Currently returns null which
            //  will skip archiving the message. Consider showing a notification or error message.
            null
        }
    }
}
