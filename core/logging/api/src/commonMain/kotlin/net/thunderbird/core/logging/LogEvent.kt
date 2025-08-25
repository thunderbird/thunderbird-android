package net.thunderbird.core.logging

typealias LogTag = String
typealias LogMessage = String

/**
 * Represents a single log event
 *
 * @property level The [LogLevel] of the log event.
 * @property tag An optional [LogTag] to categorize the log event.
 * @property message The [LogMessage] associated with the log event.
 * @property throwable An optional [Throwable] associated with the log event.
 * @property timestamp The timestamp of the log event in milliseconds.
 */
data class LogEvent(
    val level: LogLevel,
    val tag: LogTag? = null,
    val message: LogMessage,
    val throwable: Throwable? = null,
    val timestamp: Long,
)
