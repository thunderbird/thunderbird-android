package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.Preferences

class FolderRepositoryManager(
    private val localStoreProvider: LocalStoreProvider,
    private val preferences: Preferences
) {
    fun getFolderRepository(account: Account) = FolderRepository(localStoreProvider, preferences, account)
}
