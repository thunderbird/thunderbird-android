package net.thunderbird.core.logging

/**
 * Provides the current [LogLevel].
 *
 * This can be used to dynamically change the log level during runtime.
 */
fun interface LogLevelProvider {
    /**
     * Gets the current log level.
     *
     * @return The current log level.
     */
    fun current(): LogLevel
}
