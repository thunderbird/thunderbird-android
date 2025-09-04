package net.thunderbird.core.configstore

/**
 * Represents a key for a configuration value.
 *
 * This sealed class defines different types of configuration keys that can be used
 * to store and retrieve values in a configuration store.
 *
 * @param T The type of the value associated with this key.
 * @property name The name of the configuration key.
 */
sealed class ConfigKey<T>(val name: String) {
    class BooleanKey(name: String) : ConfigKey<Boolean>(name)
    class IntKey(name: String) : ConfigKey<Int>(name)
    class StringKey(name: String) : ConfigKey<String>(name)
    class LongKey(name: String) : ConfigKey<Long>(name)
    class FloatKey(name: String) : ConfigKey<Float>(name)
    class DoubleKey(name: String) : ConfigKey<Double>(name)
}
