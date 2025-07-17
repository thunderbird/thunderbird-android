package net.thunderbird.core.configstore.backend

import net.thunderbird.core.configstore.ConfigId

/**
 * Factory interface for creating [ConfigBackend] instances.
 */
interface ConfigBackendFactory {
    /**
     * Creates a [ConfigBackend].
     *
     * @param id The [ConfigId] for used to identify the [ConfigBackend].
     * @return A [ConfigBackend] instance.
     */
    fun create(id: ConfigId): ConfigBackend
}
