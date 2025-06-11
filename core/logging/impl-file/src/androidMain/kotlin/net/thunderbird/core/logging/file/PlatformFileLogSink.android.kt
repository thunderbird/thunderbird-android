package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

internal actual fun platformFileLogSink(level: LogLevel, tagFilters: Array<String>?, messageFilter: String?, fileName: String, fileLocation: String): LogSink {
    return AndroidFileLogSink(level, tagFilters, messageFilter, fileName, fileLocation)
}
