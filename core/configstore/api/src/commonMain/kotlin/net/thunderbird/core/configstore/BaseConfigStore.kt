package net.thunderbird.core.configstore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import net.thunderbird.core.configstore.backend.ConfigBackend
import net.thunderbird.core.configstore.backend.ConfigBackendProvider

/**
 * Base class for configuration stores that provides common functionality for managing configuration data.
 *
 * @param T The type of configuration data.
 * @param provider The [net.thunderbird.core.configstore.backend.ConfigBackendProvider] that provides or creates the [net.thunderbird.core.configstore.backend.ConfigBackend] for storing configuration data.
 * @param definition The definition that describes how to map the configuration data to and from the backend.
 */
abstract class BaseConfigStore<T>(
    private val provider: ConfigBackendProvider,
    private val definition: ConfigDefinition<T>,
) : ConfigStore<T> {

    private val backend: ConfigBackend by lazy {
        provider.provide(definition.id)
    }

    override val config: Flow<T?> = backend.read(definition.keys).map {
        definition.mapper.fromConfig(it) ?: definition.defaultValue
    }.distinctUntilChanged()

    override suspend fun update(transform: (T?) -> T) {
        backend.update(definition.keys) { config ->
            val current = definition.mapper.fromConfig(config) ?: definition.defaultValue
            definition.mapper.toConfig(transform(current))
        }
    }

    override suspend fun clear() = backend.clear()
}
