package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * A [LogSink] implementation that logs messages to the console.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 */
class ConsoleLogSink(
    override val level: LogLevel,
) : LogSink {
    private val platformSink = platformLogSink(level)

    override fun log(event: LogEvent) {
        platformSink.log(event)
    }
}
