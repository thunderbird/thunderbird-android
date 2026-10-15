package com.fsck.k9.mailstore

import net.thunderbird.feature.account.AccountId

interface StorageFilesProviderFactory {
    fun createStorageFilesProvider(accountId: AccountId): StorageFilesProvider
}
