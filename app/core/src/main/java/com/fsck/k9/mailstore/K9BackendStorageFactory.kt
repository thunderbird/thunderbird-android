package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences

class K9BackendStorageFactory(
        private val preferences: Preferences,
        private val folderRepositoryManager: FolderRepositoryManager
) {
    fun createBackendStorage(account: Account): K9BackendStorage {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        val specialFolderUpdater = SpecialFolderUpdater(preferences, folderRepository, account)
        return K9BackendStorage(preferences, account, account.localStore, specialFolderUpdater)
    }
}
