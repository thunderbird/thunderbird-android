package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * A [LogSink] implementation that logs messages to a specified internal file.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @param fileName The [String] fileName to log to
 */
class FileLogSink(
    override val level: LogLevel,
    tagFilters: Array<String>?,
    private val messageFilter: String?,
    private val fileName: String,
    fileLocation: String,
) : LogSink {
    private val platformSink = platformFileLogSink(level, tagFilters, messageFilter, fileName, fileLocation)

    override fun log(event: LogEvent) {
        platformSink.log(event)
    }
}
