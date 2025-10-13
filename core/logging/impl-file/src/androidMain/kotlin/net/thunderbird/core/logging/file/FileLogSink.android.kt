package net.thunderbird.core.logging.file

import net.thunderbird.core.file.FileSystemManager
import net.thunderbird.core.logging.LogLevel

actual fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    fileSystemManager: FileSystemManager,
): FileLogSink {
    return AndroidFileLogSink(level, fileName, fileLocation, fileSystemManager)
}
