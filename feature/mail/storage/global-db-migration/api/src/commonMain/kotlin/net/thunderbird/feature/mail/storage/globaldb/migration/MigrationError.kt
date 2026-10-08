package net.thunderbird.feature.mail.storage.globaldb.migration

/**
 * Represents errors that can occur during the global database migration process.
 */
public sealed interface MigrationError {

    /**
     * Indicates that there is insufficient storage available for the migration process.
     *
     * @param reason A user-safe explanation of why there is insufficient storage.
     */
    public data class InsufficientStorage(val reason: String) : MigrationError
}
