package net.thunderbird.app.common.account.data

import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.account.AccountId

/**
 * Local data source for reading and writing [LegacyAccount] instances to persistent storage.
 */
internal interface AccountLocalDataSource {

    /**
     * Loads all accounts from persistent storage in order.
     *
     * @return A list of all stored [LegacyAccount] instances.
     */
    fun loadAll(): List<LegacyAccount>

    /**
     * Loads the account with the specified [accountId] from storage.
     *
     * @param accountId The unique identifier of the account to load.
     * @return The loaded [LegacyAccount], or null if no account exists with the given ID.
     */
    fun getById(accountId: AccountId): LegacyAccount?

    /**
     * Saves the specified [account] to persistent storage.
     *
     * @param account The [LegacyAccount] to save.
     */
    fun save(account: LegacyAccount)

    /**
     * Deletes the account with the specified [accountId] from persistent storage.
     *
     * @param accountId The unique identifier of the account to delete.
     */
    fun delete(accountId: AccountId)
}
