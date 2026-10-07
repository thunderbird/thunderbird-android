package net.thunderbird.feature.account.internal.cache

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.common.cache.Cache
import net.thunderbird.core.common.cache.InMemoryCache
import net.thunderbird.core.common.cache.SynchronizedCache
import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId

/**
 * An in-memory cache for [Account] instances in the data layer.
 */
class InMemoryAccountCache<TAccount : Account>(
    private val delegateCache: Cache<AccountId, TAccount> = SynchronizedCache(
        delegateCache = InMemoryCache(),
    ),
) : AccountCache<TAccount> {

    private val accountsFlow = MutableStateFlow<List<TAccount>>(emptyList())
    override fun observeAll(): StateFlow<List<TAccount>> = accountsFlow.asStateFlow()

    override fun findAll(): List<TAccount> = accountsFlow.value

    override fun findById(accountId: AccountId): TAccount? {
        return delegateCache[accountId]
    }

    override fun update(account: TAccount) {
        delegateCache[account.id] = account
        accountsFlow.update { current ->
            val index = current.indexOfFirst { it.id == account.id }
            if (index != -1) {
                current.toMutableList().apply { set(index, account) }
            } else {
                current + account
            }
        }
    }

    override fun updateAll(accounts: List<TAccount>) {
        delegateCache.clear()
        accounts.forEach { account ->
            delegateCache[account.id] = account
        }
        accountsFlow.value = accounts
    }

    override fun delete(accountId: AccountId) {
        accountsFlow.update { current ->
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
        accountsFlow.value = emptyList()
    }
}
