package com.fsck.k9.backend

import com.fsck.k9.backend.api.Backend
import com.fsck.k9.mail.ServerSettings
import java.util.concurrent.CopyOnWriteArraySet
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class BackendManager(
    private val backendFactories: Map<String, BackendFactory>,
    private val accountManager: LegacyAccountManager,
) {
    private val backendCache = mutableMapOf<String, BackendContainer>()
    private val listeners = CopyOnWriteArraySet<BackendChangedListener>()

    // TODO remove this once Java callers have been converted to Kotlin
    fun getBackend(accountUuid: String): Backend {
        return getBackend(AccountIdFactory.of(accountUuid))
    }

    fun getBackend(accountId: AccountId): Backend {
        val newBackend = synchronized(backendCache) {
            val container = backendCache[accountId.toString()]
            val account = getAccountById(accountId)
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

        notifyListeners(accountId)

        return newBackend
    }

    private fun getAccountById(accountId: AccountId): LegacyAccount = runBlocking {
        accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
    }

    private fun isBackendStillValid(container: BackendContainer, account: LegacyAccount): Boolean {
        return container.incomingServerSettings == account.incomingServerSettings &&
            container.outgoingServerSettings == account.outgoingServerSettings
    }

    fun removeBackend(accountId: AccountId) {
        synchronized(backendCache) {
            backendCache.remove(accountId.toString())
        }

        notifyListeners(accountId)
    }

    private fun createBackend(account: LegacyAccount): Backend {
        val serverType = account.incomingServerSettings.type
        val backendFactory = backendFactories[serverType] ?: error("Unsupported account type")
        return backendFactory.createBackend(account.id)
    }

    fun addListener(listener: BackendChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: BackendChangedListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(accountId: AccountId) {
        for (listener in listeners) {
            listener.onBackendChanged(accountId)
        }
    }
}

private data class BackendContainer(
    val backend: Backend,
    val incomingServerSettings: ServerSettings,
    val outgoingServerSettings: ServerSettings,
)

fun interface BackendChangedListener {
    fun onBackendChanged(accountId: AccountId)
}
