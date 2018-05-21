package com.fsck.k9.mailstore

import com.fsck.k9.Account

class FolderRepositoryManager {
    fun getFolderRepository(account: Account) = FolderRepository(account)
}
