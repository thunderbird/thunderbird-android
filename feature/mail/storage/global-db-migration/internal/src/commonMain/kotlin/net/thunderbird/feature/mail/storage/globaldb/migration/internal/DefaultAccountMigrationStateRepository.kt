package net.thunderbird.feature.mail.storage.globaldb.migration.internal

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.storage.globaldb.migration.AccountMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.AccountMigrationStateRepository
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationStateRepository

/**
 * Placeholder implementation of [AccountMigrationStateRepository].
 *
 * Missing:
 * - Persistence based lookup
 * - Persistence based update
 */
public class DefaultAccountMigrationStateRepository(
    private val appMigrationStateRepository: AppMigrationStateRepository,
) : AccountMigrationStateRepository {

    override suspend fun getByAccountId(accountId: AccountId): AccountMigrationState {
        // early bailout if the app migration is completed
        if (appMigrationStateRepository.get() == AppMigrationState.Completed) {
            return AccountMigrationState.Completed
        }

        // TODO implement persistence based lookup

        return AccountMigrationState.NotStarted
    }

    override suspend fun update(
        accountId: AccountId,
        state: AccountMigrationState,
    ) {
        TODO("Not yet implemented")
    }
}
