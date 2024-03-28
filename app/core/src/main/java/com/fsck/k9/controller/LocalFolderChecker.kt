package com.fsck.k9.controller

import com.fsck.k9.Account
import com.fsck.k9.mailstore.FolderRepository

/**
 * Checks whether a folder is only available locally.
 */
internal fun interface LocalFolderChecker {
    fun isLocalFolder(account: Account, folderId: Long): Boolean
}

internal class DefaultLocalFolderChecker(private val folderRepository: FolderRepository) : LocalFolderChecker {
    override fun isLocalFolder(account: Account, folderId: Long): Boolean {
        return folderRepository.isLocalFolder(account, folderId)
    }
}
