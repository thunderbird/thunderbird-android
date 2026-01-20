package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.account.api.AccountManager

interface LegacyAccountManager : AccountManager<LegacyAccount> {
    /**
     * Returns a flow of all accounts.
     */
    fun getAll(): Flow<List<LegacyAccount>>

    /**
     * Returns a flow of the account with the specified [id].
     *
     * @param id The ID of the account.
     */
    fun getById(id: AccountId): Flow<LegacyAccount?>

    /**
     * Updates the specified [account].
     *
     * @param account The account to update.
     */
    suspend fun update(account: LegacyAccount)

    /**
     * Returns the account with the specified [id] synchronously.
     *
     * @param id The ID of the account.
     */
    fun getByIdSync(id: AccountId): LegacyAccount?

    /**
     * Updates the specified [account] synchronously.
     *
     * @param account The account to update.
     */
    fun updateSync(account: LegacyAccount)
}
