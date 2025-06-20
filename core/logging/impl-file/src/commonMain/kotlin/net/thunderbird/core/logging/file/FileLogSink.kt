package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

interface FileLogSink : LogSink {
    /**
     * Exports from the logging method to the requested external file
     *
     * @param uriString The [String] for the URI to export the log to
     *
     * **/
    fun export(uriString: String)

    /**
     * On a crash, flushes buffer to file fo avoid log loss
     *
     * **/
    fun flushAndCloseBuffer()
}

/**
 * A [LogSink] implementation that logs messages to a specified internal file.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @param fileName The [String] fileName to log to
 */
expect fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    configuration: PlatformConfig,
): FileLogSink
