package net.thunderbird.core.android.account

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId

interface LegacyAccountRepository {
    /**
     * Returns a flow of the account with the specified [accountId].
     *
     * @param accountId The ID of the account.
     */
    fun observeById(accountId: AccountId): Flow<LegacyAccount?>

    /**
     * Updates the specified [account].
     *
     * @param account The account to update.
     */
    suspend fun update(account: LegacyAccount)
}
