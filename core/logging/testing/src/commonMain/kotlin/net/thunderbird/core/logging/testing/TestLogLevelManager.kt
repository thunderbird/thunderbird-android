package net.thunderbird.core.logging.testing

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager

class TestLogLevelManager : LogLevelManager {
    var logLevel = LogLevel.VERBOSE
    override fun override(level: LogLevel) {
        logLevel = level
    }

    override fun restoreDefault() {
        logLevel = LogLevel.VERBOSE
    }

    override fun current(): LogLevel = logLevel
}
