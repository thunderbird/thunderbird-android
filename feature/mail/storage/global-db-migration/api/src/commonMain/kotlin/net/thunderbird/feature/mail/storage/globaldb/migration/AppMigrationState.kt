package net.thunderbird.feature.mail.storage.globaldb.migration

/**
 * Represents the current state of the global database migration process for the application.
 */
public sealed interface AppMigrationState {

    /**
     * The migration has not yet started.
     */
    public data object NotStarted : AppMigrationState

    /**
     * The migration is currently in progress.
     *
     * @param progress A fraction between 0.0 and 1.0 indicating how much of the overall app migration is complete.
     */
    public data class InProgress(val progress: Float) : AppMigrationState {
        init {
            require(progress in 0.0f..1.0f) { "Progress must be between 0.0 and 1.0" }
        }
    }

    /**
     * The migration completed successfully.
     */
    public data object Completed : AppMigrationState

    /**
     * The migration failed.
     *
     * @param error The specific error that caused the migration to fail.
     */
    public data class Failed(val error: MigrationError) : AppMigrationState
}
