package net.thunderbird.feature.mail.storage.globaldb.migration

/**
 * Represents the current state of the data migration for a specific account.
 */
public sealed interface AccountMigrationState {

    /**
     * The account has not yet been migrated and is using the legacy database implementation.
     */
    public data object NotStarted : AccountMigrationState

    /**
     * The migration is currently in progress.
     *
     * @param progress A fraction between 0.0 and 1.0 indicating how much of this account's migration is complete.
     */
    public data class InProgress(val progress: Float) : AccountMigrationState {
        init {
            require(progress in 0.0f..1.0f) { "Progress must be between 0.0 and 1.0" }
        }
    }

    /**
     * The migration completed successfully and the account is using the new database.
     */
    public data object Completed : AccountMigrationState

    /**
     * The migration failed.
     *
     * @param error The specific error that caused the migration to fail.
     */
    public data class Failed(val error: MigrationError) : AccountMigrationState
}
