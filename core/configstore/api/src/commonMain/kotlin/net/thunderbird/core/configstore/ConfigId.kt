package net.thunderbird.core.configstore

/**
 * Represents a unique identifier for a configuration.
 *
 * @property value The string value of the configuration ID.
 * @throws IllegalArgumentException if the value is blank or contains invalid characters.
 */
@JvmInline
value class ConfigId(val value: String) {
    init {
        require(value.isNotBlank()) {
            "ConfigId cannot be blank"
        }
        require(value.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            "ConfigId must only contain alphanumeric characters or underscores"
        }
    }
}
