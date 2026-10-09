package com.fsck.k9.controller

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.createFolder
import com.fsck.k9.backend.api.updateFolders
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.Logger

internal class BackendStorageArchiveFolderCreator(
    private val backendStorageFactory: BackendStorageFactory,
    private val logger: Logger,
) : ArchiveFolderCreator {
    @Suppress("TooGenericExceptionCaught")
    override fun createFolder(account: LegacyAccountDto, folderInfo: FolderInfo): Long? {
        return try {
            val backendStorage = backendStorageFactory.createBackendStorage(account.id)
            backendStorage.updateFolders {
                createFolder(folderInfo)
            }
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failed to create archive subfolder: ${folderInfo.serverId}" }
            null
        }
    }
}
