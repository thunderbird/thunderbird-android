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
     * Updates the specified [account].
     *
     * @param account The account to update.
     */
    suspend fun update(account: LegacyAccount)

    /**
     * Updates the specified [account] synchronously.
     *
     * @param account The account to update.
     */
    fun updateSync(account: LegacyAccount)
}
