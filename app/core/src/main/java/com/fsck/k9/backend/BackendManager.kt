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
                    backendCache[account.uuid] = BackendContainer(
                        backend,
                        account.incomingServerSettings,
                        account.outgoingServerSettings
                    )
                }
            }
        }
    }

    private fun isBackendStillValid(container: BackendContainer, account: Account): Boolean {
        return container.incomingServerSettings == account.incomingServerSettings &&
            container.outgoingServerSettings == account.outgoingServerSettings
    }

    fun removeBackend(account: Account) {
        synchronized(backendCache) {
            backendCache.remove(account.uuid)
        }
    }

    private fun createBackend(account: Account): Backend {
        val serverType = account.incomingServerSettings.type
        val backendFactory = backendFactories[serverType] ?: error("Unsupported account type")
        return backendFactory.createBackend(account)
    }

    private data class BackendContainer(
        val backend: Backend,
        val incomingServerSettings: ServerSettings,
        val outgoingServerSettings: ServerSettings
    )
}
