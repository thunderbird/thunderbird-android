package com.fsck.k9.backend

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings
import java.util.concurrent.CopyOnWriteArraySet
import net.thunderbird.core.android.account.LegacyAccountDto

class BackendManager(private val backendFactories: Map<String, BackendFactory>) {
    private val backendCache = mutableMapOf<String, BackendContainer>()
    private val listeners = CopyOnWriteArraySet<BackendChangedListener>()

    fun getBackend(account: LegacyAccountDto): Backend {
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

    private fun isBackendStillValid(container: BackendContainer, account: LegacyAccountDto): Boolean {
        return container.incomingServerSettings == account.incomingServerSettings &&
            container.outgoingServerSettings == account.outgoingServerSettings
    }

    fun removeBackend(account: LegacyAccountDto) {
        synchronized(backendCache) {
            backendCache.remove(account.uuid)
        }

        notifyListeners(account)
    }

    private fun createBackend(account: LegacyAccountDto): Backend {
        val serverType = account.incomingServerSettings.type
        val backendFactory = backendFactories[serverType] ?: error("Unsupported account type")
        return backendFactory.createBackend(account)
    }

    fun addListener(listener: BackendChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BackendChangedListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(account: LegacyAccountDto) {
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

fun interface BackendChangedListener {
    fun onBackendChanged(account: LegacyAccountDto)
}
