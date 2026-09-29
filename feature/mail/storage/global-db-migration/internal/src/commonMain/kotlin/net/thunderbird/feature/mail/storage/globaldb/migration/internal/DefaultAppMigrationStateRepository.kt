package net.thunderbird.feature.mail.storage.globaldb.migration.internal

import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationStateRepository

/**
 * Placeholder implementation of [AppMigrationStateRepository].
 *
 * Missing:
 * - Persistence based lookup
 * - Persistence based update
 */
public class DefaultAppMigrationStateRepository : AppMigrationStateRepository {
    override suspend fun get(): AppMigrationState {
        // TODO implement persistence based lookup

        return AppMigrationState.NotStarted
    }

    override suspend fun update(state: AppMigrationState) {
        TODO("Not yet implemented")
    }
}
