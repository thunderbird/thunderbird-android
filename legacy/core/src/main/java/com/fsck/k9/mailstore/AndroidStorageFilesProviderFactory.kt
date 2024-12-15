package com.fsck.k9.mailstore

import android.content.Context

class AndroidStorageFilesProviderFactory(
    private val context: Context,
) : StorageFilesProviderFactory {
    override fun createStorageFilesProvider(accountId: String): StorageFilesProvider {
        return AndroidStorageFilesProvider(context, accountId)
    }
}
