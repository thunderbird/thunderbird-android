package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend

class BackendManager(private val backendFactories: Map<String, BackendFactory>) {
    private val backendCache = mutableMapOf<String, Backend>()


    fun getBackend(account: Account): Backend {
        synchronized (backendCache) {
            return backendCache[account.uuid] ?: createBackend(account).also { backendCache[account.uuid] = it }
        }
    }

    fun removeBackend(account: Account) {
        synchronized (backendCache) {
            backendCache.remove(account.uuid)
        }
    }

    private fun createBackend(account: Account): Backend {
        val storeUri = account.storeUri
        backendFactories.forEach { (storeUriPrefix, backendFactory) ->
            if (storeUri.startsWith(storeUriPrefix)) {
                return backendFactory.createBackend(account)
            }
        }

        throw IllegalArgumentException("Unsupported account type")
    }
}
