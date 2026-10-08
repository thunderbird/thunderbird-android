package net.thunderbird.piisafe.annotation.options

/**
 * Defines the command line options available for the PII-safe compiler plugin.
 * Each enum constant represents a specific configuration option that can be passed
 * to the plugin during compilation.
 */
public enum class PiiSafeCliOption(public val optionName: String) {
    ENABLED("enabled"),
}
