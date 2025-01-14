package com.fsck.k9.mailstore

interface StorageFilesProviderFactory {
    fun createStorageFilesProvider(accountId: String): StorageFilesProvider
}
