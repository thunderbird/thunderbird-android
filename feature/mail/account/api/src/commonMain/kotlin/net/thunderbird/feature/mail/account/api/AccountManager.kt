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

    /**
     * Atomically updates the account with the specified [accountId].
     *
     * [transform] receives the latest version of the account, and its result is saved. Unlike reading the account,
     * changing it and calling [update] with it, no change made elsewhere in the meantime is lost.
     *
     * [transform] may run while other account operations wait, so it must be quick and must not call this manager.
     *
     * The default implementation is not atomic. Implementations used from several threads must override it.
     *
     * @param accountId The [AccountId] of the account to update.
     * @param transform Returns the updated account for the latest version of the account.
     * @return The saved account, or `null` if no account with the specified [accountId] exists.
     */
    suspend fun update(accountId: AccountId, transform: (TAccount) -> TAccount): TAccount? {
        val account = findById(accountId) ?: return null
        return transform(account).also { update(it) }
    }

    /**
     * Synchronous version of [update] with a [transform].
     *
     * @see update
     */
    fun updateSync(accountId: AccountId, transform: (TAccount) -> TAccount): TAccount? {
        val account = findById(accountId) ?: return null
        return transform(account).also { updateSync(it) }
    }
}
