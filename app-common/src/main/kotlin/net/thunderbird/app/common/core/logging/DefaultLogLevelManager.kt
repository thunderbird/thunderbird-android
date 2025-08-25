package net.thunderbird.app.common.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.app.common.BuildConfig
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager

class DefaultLogLevelManager : LogLevelManager {
    private val defaultLevel = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.INFO
    private val logLevel = MutableStateFlow(defaultLevel)

    override fun override(level: LogLevel) {
        logLevel.update { level }
    }

    override fun restoreDefault() {
        override(defaultLevel)
    }

    override fun current(): LogLevel = logLevel.value
}
