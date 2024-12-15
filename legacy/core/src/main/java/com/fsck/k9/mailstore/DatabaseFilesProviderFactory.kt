package com.fsck.k9.mailstore

interface DatabaseFilesProviderFactory {
    fun createDatabaseFilesProvider(accountId: String): DatabaseFilesProvider
}
