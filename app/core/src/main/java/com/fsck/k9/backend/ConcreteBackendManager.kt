package com.fsck.k9.backend

import com.fsck.k9.Account
import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings
import java.util.concurrent.CopyOnWriteArraySet

class ConcreteBackendManager(private val backendFactories: Map<String, BackendFactory>) : BackendManager {
    private val backendCache = mutableMapOf<String, BackendContainer>()
    private val listeners = CopyOnWriteArraySet<BackendChangedListener>()

    override fun getBackend(account: Account): Backend {
        val newBackend = synchronized(backendCache) {
            val container = backendCache[account.uuid]
            if (container != null && isBackendStillValid(container, account)) {
                return container.backend
            }

            createBackend(account).also { backend ->
                backendCache[account.uuid] = BackendContainer(
                    backend,
                    account.incomingServerSettings,
                    account.outgoingServerSettings,
                )
            }
        }

        notifyListeners(account)

        return newBackend
    }

    private fun isBackendStillValid(container: BackendContainer, account: Account): Boolean {
        return container.incomingServerSettings == account.incomingServerSettings &&
            container.outgoingServerSettings == account.outgoingServerSettings
    }

    override fun removeBackend(account: Account) {
        synchronized(backendCache) {
            backendCache.remove(account.uuid)
        }

        notifyListeners(account)
    }

    private fun createBackend(account: Account): Backend {
        val serverType = account.incomingServerSettings.type
        val backendFactory = backendFactories[serverType] ?: error("Unsupported account type")
        return backendFactory.createBackend(account)
    }

    override fun addListener(listener: BackendChangedListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: BackendChangedListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(account: Account) {
        for (listener in listeners) {
            listener.onBackendChanged(account)
        }
    }
}

private data class BackendContainer(
    val backend: Backend,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
)
