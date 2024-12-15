package com.fsck.k9.mailstore

import android.content.Context

class AndroidDatabaseFilesProviderFactory(
    private val context: Context,
) : DatabaseFilesProviderFactory {
    override fun createDatabaseFilesProvider(accountId: String): DatabaseFilesProvider {
        return AndroidDatabaseFilesProvider(context, accountId)
    }
}
