package net.thunderbird.feature.account.internal.cache

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.account.Account
import net.thunderbird.feature.account.AccountId

/**
 * In-memory cache for [Account] instances in the data layer.
 */
interface AccountCache<TAccount : Account> {

    /**
     * Returns all cached accounts in order.
     */
    fun findAll(): List<TAccount>

    /**
     * Emits the list of cached accounts reactively.
     */
    fun observeAll(): StateFlow<List<TAccount>>

    /**
     * Returns the cached account with the specified [accountId], or null if not cached.
     */
    fun findById(accountId: AccountId): TAccount?

    /**
     * Updates or inserts the specified [account] in the cache.
     */
    fun update(account: TAccount)

    /**
     * Replaces the entire list of cached accounts.
     */
    fun updateAll(accounts: List<TAccount>)

    /**
     * Deletes the account with [accountId] from the cache.
     */
    fun delete(accountId: AccountId)

    /**
     * Deletes all cached accounts.
     */
    fun deleteAll()
}
