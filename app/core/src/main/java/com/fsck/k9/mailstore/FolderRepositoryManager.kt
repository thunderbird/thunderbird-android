package com.fsck.k9.mailstore

import com.fsck.k9.Account

class FolderRepositoryManager(
    private val localStoreProvider: LocalStoreProvider,
    private val specialFolderSelectionStrategy: SpecialFolderSelectionStrategy
) {
    fun getFolderRepository(account: Account) = FolderRepository(localStoreProvider, account)
}
