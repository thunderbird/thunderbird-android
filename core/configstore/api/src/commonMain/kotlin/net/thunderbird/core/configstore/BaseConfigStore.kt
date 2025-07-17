package net.thunderbird.core.configstore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

/**
 * Base class for configuration stores that provides common functionality for managing configuration data.
 *
 * @param T The type of configuration data.
 * @param provider The [ConfigBackendProvider] that provides or creates the [ConfigBackend] for storing config data.
 * @param definition The definition that describes how to map the configuration data to and from the backend.
 */
abstract class BaseConfigStore<T>(
    private val provider: ConfigBackendProvider,
    private val definition: ConfigDefinition<T>,
) : ConfigStore<T> {

    private var migrationPerformed = false

    private val backend: ConfigBackend by lazy {
        provider.provide(definition.id)
    }

    override val config: Flow<T> = backend.read(definition.keys)
        .onStart { ensureMigration() }
        .map { definition.mapper.fromConfig(it) ?: definition.defaultValue }
        .distinctUntilChanged()

    override suspend fun update(transform: (T?) -> T) {
        backend.update(definition.keys) { config ->
            val current = definition.mapper.fromConfig(config) ?: definition.defaultValue
            definition.mapper.toConfig(transform(current))
        }
    }

    override suspend fun clear() = backend.clear()

    private fun getVersionKey(): String {
        val id = definition.id
        return "_version_${id.backend}_${id.feature}"
    }

    private suspend fun ensureMigration() {
        if (migrationPerformed) return
        val versionKey = getVersionKey()
        val currentVersion = backend.readVersion(versionKey)

        if (currentVersion < definition.version) {
            val currentConfig = backend.read(definition.keys).first()
            val result = definition.migration.migrate(currentVersion, definition.version, currentConfig)
            when (result) {
                is ConfigMigrationResult.Migrated -> {
                    backend.update(definition.keys) { result.updated }
                    backend.removeKeys(result.keysToRemove)
                }

                ConfigMigrationResult.NoOp -> {
                    // No migration needed, just ensure the version is updated
                }
            }

            // Write the new version to the backend
            backend.writeVersion(versionKey, definition.version)

            migrationPerformed = true
        }
    }
}
