package net.thunderbird.core.configstore

/**
 * Interface for mapping configuration objects to a [Config] representation and vice versa.
 *
 * @param T The type of the configuration object.
 */
interface ConfigMapper<T> {
    /**
     * Maps a configuration object to a [Config] representation.
     *
     * @param obj The configuration object to map.
     * @return The [Config] representation of the configuration.
     */
    fun toConfig(obj: T): Config

    /**
     * Maps a string representation back to a configuration object.
     *
     * @param config The [Config] to map from.
     * @return The configuration object.
     */
    fun fromConfig(config: Config): T?
}
