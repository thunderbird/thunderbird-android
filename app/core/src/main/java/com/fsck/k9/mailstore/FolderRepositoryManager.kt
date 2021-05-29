package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.preferences.AccountManager

class FolderRepositoryManager(
    private val messageStoreManager: MessageStoreManager,
    private val accountManager: AccountManager
) {
    fun getFolderRepository(account: Account): FolderRepository {
        return FolderRepository(messageStoreManager, accountManager, account)
    }
}
