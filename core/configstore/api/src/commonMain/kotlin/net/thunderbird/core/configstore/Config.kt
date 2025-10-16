package net.thunderbird.core.configstore

/**
 * A configuration holds key-value pairs of [ConfigKey] and their corresponding values.
 *
 * It is used to store and retrieve configuration settings in a type-safe manner.
 */
class Config(
    private val entries: MutableMap<ConfigKey<*>, Any?> = mutableMapOf(),
) {
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: ConfigKey<T>): T? = entries[key] as? T

    operator fun <T> set(key: ConfigKey<T>, value: T) {
        entries[key] = value
    }

    /**
     * Returns a map representation of the configuration.
     */
    fun toMap(): Map<ConfigKey<*>, Any?> = entries.toMap()
}
