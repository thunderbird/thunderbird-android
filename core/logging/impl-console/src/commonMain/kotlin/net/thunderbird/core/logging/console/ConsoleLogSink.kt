package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * A [LogSink] implementation that logs messages to the console.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 */
interface ConsoleLogSink : LogSink

/**
 * Creates a [ConsoleLogSink] with the specified log level.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @return A new instance of [ConsoleLogSink].
 */
expect fun ConsoleLogSink(
    level: LogLevel = LogLevel.INFO,
): ConsoleLogSink
