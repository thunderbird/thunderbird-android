package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.core.common.cache.SynchronizedCache
import net.thunderbird.feature.account.AccountId

/**
 * An in-memory cache for [LegacyAccount] instances in the data layer.
 */
internal class LegacyInMemoryAccountCache(
    private val delegateCache: Cache<AccountId, LegacyAccount> = SynchronizedCache(
        delegateCache = InMemoryCache(),
    ),
) : LegacyAccountCache {

    private val accountsFlowState = MutableStateFlow<List<LegacyAccount>>(emptyList())
    override fun observeAll(): StateFlow<List<LegacyAccount>> = accountsFlowState.asStateFlow()

    override fun findAll(): List<LegacyAccount> = accountsFlowState.value

    override fun findById(accountId: AccountId): LegacyAccount? {
        return delegateCache[accountId]
    }

    override fun update(account: LegacyAccount) {
        delegateCache[account.id] = account
        accountsFlowState.update { current ->
            val index = current.indexOfFirst { it.id == account.id }
            if (index != -1) {
                current.toMutableList().apply { set(index, account) }
            } else {
                current + account
            }
        }
    }

    override fun updateAll(accounts: List<LegacyAccount>) {
        delegateCache.clear()
        accounts.forEach { account ->
            delegateCache[account.id] = account
        }
        accountsFlowState.value = accounts
    }

    override fun delete(accountId: AccountId) {
        accountsFlowState.update { current ->
            val updated = current.filterNot { it.id == accountId }
            delegateCache.clear()
            updated.forEach { account ->
                delegateCache[account.id] = account
            }
            updated
        }
    }

    override fun deleteAll() {
        delegateCache.clear()
        accountsFlowState.value = emptyList()
    }
}
