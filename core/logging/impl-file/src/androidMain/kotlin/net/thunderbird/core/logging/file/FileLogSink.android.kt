package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogLevel

actual fun FileLogSink(
    level: LogLevel,
    fileName: String,
    fileLocation: String,
    configuration: PlatformConfig,
): FileLogSink {
    return AndroidFileLogSink(level, fileName, fileLocation, configuration.contentResolver)
}
