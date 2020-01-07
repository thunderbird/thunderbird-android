package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences

class K9BackendStorageFactory(
    private val preferences: Preferences,
    private val folderRepositoryManager: FolderRepositoryManager,
    private val localStoreProvider: LocalStoreProvider,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy
) {
    fun createBackendStorage(account: Account): K9BackendStorage {
        val folderRepository = folderRepositoryManager.getFolderRepository(account)
        val localStore = localStoreProvider.getInstance(account)
        val specialFolderUpdater = SpecialFolderUpdater(
            preferences,
            folderRepository,
            specialFolderSelectionStrategy,
            account
        )
        val specialFolderListener = SpecialFolderBackendStorageListener(specialFolderUpdater)
        val autoExpandFolderListener = AutoExpandFolderBackendStorageListener(preferences, account)
        val listeners = listOf(specialFolderListener, autoExpandFolderListener)
        return K9BackendStorage(preferences, account, localStore, listeners)
    }
}
