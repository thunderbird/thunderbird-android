package net.thunderbird.app.common.account.data

import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId

/**
 * Fast in-memory cache for [LegacyAccount] instances in the data layer.
 */
internal interface LegacyAccountCache {

    /**
     * Returns all cached accounts in order.
     */
    fun findAll(): List<LegacyAccount>

    /**
     * Emits the list of cached accounts reactively.
     */
    fun observeAll(): StateFlow<List<LegacyAccount>>

    /**
     * Returns the cached account with the specified [accountId], or null if not cached.
     */
    fun findById(accountId: AccountId): LegacyAccount?

    /**
     * Updates or inserts the specified [account] in the cache.
     */
    fun update(account: LegacyAccount)

    /**
     * Replaces the entire list of cached accounts.
     */
    fun updateAll(accounts: List<LegacyAccount>)

    /**
     * Deletes the account with [accountId] from the cache.
     */
    fun delete(accountId: AccountId)

    /**
     * Deletes all cached accounts.
     */
    fun deleteAll()
}
