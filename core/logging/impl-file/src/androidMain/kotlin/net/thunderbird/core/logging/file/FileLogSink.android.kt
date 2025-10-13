package net.thunderbird.core.logging.file

import net.thunderbird.core.file.FileManager
import net.thunderbird.core.logging.LogLevel

actual fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    fileManager: FileManager,
): FileLogSink {
    return AndroidFileLogSink(
        level = level,
        fileName = fileName,
        fileLocation = fileLocation,
        fileManager = fileManager,
    )
}
