package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

interface FileLogSink : LogSink {
    /**
     * Exports from the logging method to the requested external file
     * @param uriString The [String] for the URI to export the log to
     *
     **/
    suspend fun export(uriString: String)

    /**
     * On a crash or close, flushes buffer to file fo avoid log loss
     *
     **/
    suspend fun flushAndCloseBuffer()
}

/**
 * A [LogSink] implementation that logs messages to a specified internal file.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @param fileName The [String] fileName to log to
 * @param fileLocation The [String] fileLocation for the log file
 * @param fileSystemManager The [FileSystemManager] abstraction for opening the file stream
 */
expect fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    fileSystemManager: FileSystemManager,
): FileLogSink
