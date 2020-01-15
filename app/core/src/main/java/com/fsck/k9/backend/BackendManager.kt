package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings

class BackendManager(private val backendFactories: Map<String, BackendFactory>) {
    private val backendCache = mutableMapOf<String, BackendContainer>()

    fun getBackend(account: Account): Backend {
        synchronized(backendCache) {
            val container = backendCache[account.uuid]
            return if (container != null && isBackendStillValid(container, account)) {
                container.backend
            } else {
                createBackend(account).also { backend ->
                    backendCache[account.uuid] = BackendContainer(backend, account.storeUri, account.transportUri)
                }
            }
        }
    }

    private fun isBackendStillValid(container: BackendContainer, account: Account): Boolean {
        return container.storeUri == account.storeUri && container.transportUri == account.transportUri
    }

    fun removeBackend(account: Account) {
        synchronized(backendCache) {
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

    fun decodeStoreUri(storeUri: String): ServerSettings {
        backendFactories.forEach { (storeUriPrefix, backendFactory) ->
            if (storeUri.startsWith(storeUriPrefix)) {
                return backendFactory.decodeStoreUri(storeUri)
            }
        }

        throw IllegalArgumentException("Unsupported storeUri type")
    }

    fun createStoreUri(serverSettings: ServerSettings): String {
        backendFactories.forEach { (storeUriPrefix, backendFactory) ->
            if (serverSettings.type == storeUriPrefix) {
                return backendFactory.createStoreUri(serverSettings)
            }
        }

        throw IllegalArgumentException("Unsupported ServerSettings type")
    }

    fun decodeTransportUri(transportUri: String): ServerSettings {
        backendFactories.forEach { (_, backendFactory) ->
            if (transportUri.startsWith(backendFactory.transportUriPrefix)) {
                return backendFactory.decodeTransportUri(transportUri)
            }
        }

        throw IllegalArgumentException("Unsupported transportUri type")
    }

    fun createTransportUri(serverSettings: ServerSettings): String {
        backendFactories.forEach { (_, backendFactory) ->
            if (serverSettings.type == backendFactory.transportUriPrefix) {
                return backendFactory.createTransportUri(serverSettings)
            }
        }

        throw IllegalArgumentException("Unsupported ServerSettings type")
    }

    private data class BackendContainer(val backend: Backend, val storeUri: String, val transportUri: String)
}
