package net.thunderbird.core.configstore

/**
 * A definition of how to manage a configuration for specific type.
 *
 * The id of the configuration is used as a unique identifier to distinguish it from other configurations and
 * is used by the [net.thunderbird.core.configstore.backend.ConfigBackendProvider] to retrieve the correct configuration backend.
 *
 * This allows configurations to selectively share a backend, which can be useful for performance or organizational purposes.
 * It also allows for the creation of multiple configurations that can be managed independently, even if they share the same backend.
 *
 * @param T The type of the configuration object.
 *
 */
interface ConfigDefinition<T> {

    /**
     * The id of the configuration.
     *
     * It is used by the [net.thunderbird.core.configstore.backend.ConfigBackendProvider] to retrieve the correct configuration backend.
     */
    val id: ConfigId

    /**
     * The mapper used to convert between the configuration object and its representation in the config store.
     */
    val mapper: ConfigMapper<T>

    /**
     * The default value for the configuration.
     */
    val defaultValue: T

    /**
     * The list of keys that define the configuration.
     *
     * These keys are used to store and retrieve values in the configuration store.
     */
    val keys: List<ConfigKey<*>>
}
