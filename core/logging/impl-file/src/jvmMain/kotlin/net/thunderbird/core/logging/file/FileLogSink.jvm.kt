package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogLevel

/**
 * A [LogSink] implementation that logs messages to a specified internal file.
 *
 * This sink uses the platform-specific implementations to handle logging.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @param fileName The [String] fileName to log to
 */
actual fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    fileSystemManager: FileSystemManager,
): FileLogSink {
    return JvmFileLogSink(level, fileName, fileLocation)
}
