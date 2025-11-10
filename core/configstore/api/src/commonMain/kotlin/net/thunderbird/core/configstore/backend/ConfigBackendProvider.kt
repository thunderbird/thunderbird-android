package net.thunderbird.core.configstore.backend

import net.thunderbird.core.configstore.ConfigId

/**
 * Provider that is responsible for providing a [ConfigBackend] by its [ConfigId].
 */
interface ConfigBackendProvider {

    /**
     * Provides a [ConfigBackend] instance for the given [ConfigId].
     *
     * @param id The id of the [ConfigBackend] to provide.
     */
    fun provide(id: ConfigId): ConfigBackend
}
