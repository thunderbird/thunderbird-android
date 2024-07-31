package com.fsck.k9.backends

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.backend.BackendFactory
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.backend.ddd.DddBackend
import com.fsck.k9.mailstore.K9BackendStorageFactory

class DddBackendFactory(
    private val context: Context,
    private val backendStorageFactory: K9BackendStorageFactory,
) : BackendFactory {
    override fun createBackend(account: Account): Backend {
        val accountName = account.displayName
        val backendStorage = backendStorageFactory.createBackendStorage(account)
        return DddBackend(context, accountName, backendStorage)
    }
}
