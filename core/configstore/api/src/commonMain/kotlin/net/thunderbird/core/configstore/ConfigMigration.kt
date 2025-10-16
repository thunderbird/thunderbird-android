package net.thunderbird.core.configstore

interface ConfigMigration {

    /**
     * Migrate the configuration to a new version.
     *
     * @param currentVersion The current version of the configuration.
     * @param newVersion The new version to migrate to.
     * @param current The current configuration to be migrated.
     * @return A [ConfigMigrationResult] containing the updated configuration and any keys that should be removed.
     */
    suspend fun migrate(currentVersion: Int, newVersion: Int, current: Config): ConfigMigrationResult
}

sealed interface ConfigMigrationResult {
    /**
     * Result of a configuration migration, indicating the updated configuration and any keys that should be removed.
     *
     * @param updated The updated configuration after migration.
     * @param keysToRemove A set of configuration keys that should be removed after migration.
     */
    data class Migrated(
        val updated: Config,
        val keysToRemove: Set<ConfigKey<*>> = emptySet(),
    ) : ConfigMigrationResult

    /**
     * A no-op migration that does not change the configuration.
     */
    object NoOp : ConfigMigrationResult
}
