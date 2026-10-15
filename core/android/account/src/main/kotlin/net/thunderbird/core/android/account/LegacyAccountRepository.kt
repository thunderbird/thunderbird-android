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
     * Atomically updates the account with the specified [accountId].
     *
     * [transform] receives the latest version of the account, and its result is saved, so no change made elsewhere in
     * the meantime is lost.
     *
     * @return The saved account, or `null` if no account with the specified [accountId] exists.
     */
    suspend fun update(accountId: AccountId, transform: (LegacyAccount) -> LegacyAccount): LegacyAccount?
}
