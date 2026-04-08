package net.thunderbird.core.configstore

/**
 * A configuration holds key-value pairs of [ConfigKey] and their corresponding values.
 *
 * This is used to store and retrieve configuration settings in a type-safe manner.
 */
class Config(
    private val entries: MutableMap<ConfigKey<*>, Any?> = mutableMapOf(),
) {
    /**
     * Returns the value associated with the given [ConfigKey], or null if the key is not present.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: ConfigKey<T>): T? = entries[key] as? T

    /**
     * Sets the value for the given [ConfigKey]. The value must be of the correct type corresponding to the key.
     */
    operator fun <T> set(key: ConfigKey<T>, value: T) {
        entries[key] = value
    }

    /**
     * Returns a map representation of the configuration.
     */
    fun toMap(): Map<ConfigKey<*>, Any?> = entries.toMap()

    /**
     * Returns a copy of the configuration.
     */
    fun copy(): Config = Config(entries.toMutableMap())
}
