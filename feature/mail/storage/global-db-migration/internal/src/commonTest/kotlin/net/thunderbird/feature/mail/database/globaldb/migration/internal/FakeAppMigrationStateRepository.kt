package net.thunderbird.feature.mail.database.globaldb.migration.internal

import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationState
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationStateRepository

class FakeAppMigrationStateRepository(
    private var currentState: AppMigrationState = AppMigrationState.NotStarted,
) : AppMigrationStateRepository {
    override suspend fun get(): AppMigrationState = currentState

    override suspend fun update(state: AppMigrationState) {
        currentState = state
    }
}
