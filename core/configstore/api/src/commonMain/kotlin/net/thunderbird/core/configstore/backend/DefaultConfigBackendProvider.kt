package net.thunderbird.core.configstore.backend

import net.thunderbird.core.configstore.ConfigId

/**
 * Default implementation of [ConfigBackendProvider] that uses a [ConfigBackendFactory]
 * to create and cache [ConfigBackend] instances.
 */
class DefaultConfigBackendProvider(
    private val backendFactory: ConfigBackendFactory,
    private val backends: MutableMap<ConfigId, ConfigBackend> = mutableMapOf(),
) : ConfigBackendProvider {

    override fun provide(id: ConfigId): ConfigBackend {
        return backends[id] ?: createAndCacheBackend(id)
    }

    private fun createAndCacheBackend(id: ConfigId): ConfigBackend {
        val backend = backendFactory.create(id)
        backends[id] = backend
        return backend
    }
}
