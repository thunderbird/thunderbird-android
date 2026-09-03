package net.thunderbird.piisafe.annotation

/**
 * Defines the command line options available for the PII logging compiler plugin.
 * Each enum constant represents a specific configuration option that can be passed
 * to the plugin during compilation.
 */
public enum class PiiLoggingCliOption(public val optionName: String) {
    ENABLED("enabled"),
}
