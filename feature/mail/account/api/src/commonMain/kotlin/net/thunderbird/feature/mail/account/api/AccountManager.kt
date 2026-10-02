package net.thunderbird.feature.mail.account.api

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface AccountManager<TAccount : BaseAccount> {
    /**
     * Returns a list of all accounts.
     */
    fun getAccounts(): List<TAccount>

    /**
     * Returns a flow of all accounts.
     */
    fun getAccountsFlow(): Flow<List<TAccount>>

    /**
     * Returns the account with the specified [AccountId].
     *
     * @param accountId The [AccountId] of the account.
     * @return The account with the specified [AccountId].
     */
    fun getById(accountId: AccountId): TAccount?

    /**
     * Observe the account with the specified [AccountId].
     *
     * @param accountId The [AccountId] of the account.
     * @return The flow of the account with the specified [AccountId]
     */
    fun observeById(accountId: AccountId): Flow<TAccount?>

    /**
     * Moves the specified [account] to the [newPosition].
     *
     * @param account The account to move.
     * @param newPosition The new position of the account.
     */
    fun moveAccount(account: TAccount, newPosition: Int)

    /**
     * Saves the specified [account].
     *
     * @param account The account to save.
     */
    fun saveAccount(account: TAccount)
}
