package net.thunderbird.core.configstore

/**
 * Represents a unique identifier for a configuration.
 *
 * @property backend The backend identifier for the configuration.
 * @property feature The feature identifier for the configuration.
 * @throws IllegalArgumentException if the values are blank or contain invalid characters.
 */
data class ConfigId(
    val backend: String,
    val feature: String,
) {
    init {
        require(backend.matches(Regex("^[a-zA-Z0-9_]+$"))) { "Invalid backend name: $backend" }
        require(feature.matches(Regex("^[a-zA-Z0-9_]+$"))) { "Invalid feature name: $feature" }
    }
}
