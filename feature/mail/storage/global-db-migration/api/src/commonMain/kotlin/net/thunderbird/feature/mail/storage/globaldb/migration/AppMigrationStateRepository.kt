package net.thunderbird.feature.mail.storage.globaldb.migration

/**
 * Retrieves and updates migration state for the global application database.
 */
public interface AppMigrationStateRepository {

    /**
     * Retrieves the current migration state of the global application database.
     */
    public suspend fun get(): AppMigrationState

    /**
     * Updates the migration state of the global application database.
     *
     * @param state The new state to persist.
     */
    public suspend fun update(state: AppMigrationState)
}
