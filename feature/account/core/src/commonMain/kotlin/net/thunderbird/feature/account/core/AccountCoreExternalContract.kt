package net.thunderbird.feature.account.core

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.profile.AccountProfile

interface AccountCoreExternalContract {

    /**
     * Local data source for account profiles.
     */
    interface AccountProfileLocalDataSource {

        /**
         * Gets all account profiles as a flow.
         *
         * @return A flow emitting a list of all account profiles.
         */
        fun getAll(): Flow<List<AccountProfile>>

        /**
         * Gets an account profile by its ID as a flow.
         *
         * @param accountId The ID of the account.
         * @return A flow emitting the account profile or null if not found.
         */
        fun getById(accountId: AccountId): Flow<AccountProfile?>

        /**
         * Updates the given account profile.
         *
         * @param accountProfile The account profile to update.
         */
        suspend fun update(accountProfile: AccountProfile)
    }
}
