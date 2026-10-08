package net.thunderbird.feature.mail.account.api

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface AccountManager<TAccount : BaseAccount> {

    /**
     * Returns a list of all accounts.
     */
    fun findAll(): List<TAccount>

    /**
     * Returns a flow of all accounts.
     */
    fun observeAll(): Flow<List<TAccount>>

    /**
     * Returns the account with the specified [AccountId].
     *
     * @param accountId The [AccountId] of the account.
     * @return The account with the specified [AccountId].
     */
    fun findById(accountId: AccountId): TAccount?

    /**
     * Observe the account with the specified [AccountId].
     *
     * @param accountId The [AccountId] of the account.
     * @return The flow of the account with the specified [AccountId]
     */
    fun observeById(accountId: AccountId): Flow<TAccount?>

    /**
     * Moves the specified [AccountId] to the [newPosition].
     *
     * @param accountId The [AccountId] of the account to move.
     * @param newPosition The new position of the account.
     */
    fun moveAccount(accountId: AccountId, newPosition: Int)

    /**
     * Updates the specified [account].
     *
     * @param account The account to update.
     */
    suspend fun update(account: TAccount)

    /**
     * Updates the specified [account] synchronously.
     *
     * @param account The account to update.
     */
    fun updateSync(account: TAccount)
}
