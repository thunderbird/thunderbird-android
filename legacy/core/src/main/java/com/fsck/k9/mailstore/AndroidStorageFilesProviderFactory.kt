package com.fsck.k9.mailstore

import android.content.Context
import net.thunderbird.feature.account.AccountId

class AndroidStorageFilesProviderFactory(
    private val context: Context,
) : StorageFilesProviderFactory {
    override fun createStorageFilesProvider(accountId: AccountId): StorageFilesProvider {
        return AndroidStorageFilesProvider(context, accountId)
    }
}
