package net.thunderbird.feature.mail.storage.globaldb.migration

import net.thunderbird.feature.account.AccountId

/**
 * Retrieves and updates migration state for individual accounts.
 */
public interface AccountMigrationStateRepository {

    /**
     * Retrieves the migration state for a specific account identified by [accountId].
     *
     * @param accountId The unique identifier of the account for which the migration state is requested.
     * @return The current migration state of the specified account.
     */
    public suspend fun getByAccountId(accountId: AccountId): AccountMigrationState

    /**
     * Updates the migration state for a specific account identified by [accountId].
     *
     * @param accountId The unique identifier of the account for which the migration state is updated.
     * @param state The new migration state to be set for the specified account.
     */
    public suspend fun update(accountId: AccountId, state: AccountMigrationState)
}
