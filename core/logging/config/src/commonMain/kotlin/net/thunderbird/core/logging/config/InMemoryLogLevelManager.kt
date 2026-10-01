package net.thunderbird.core.logging.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager

class InMemoryLogLevelManager(
    private val defaultLevel: LogLevel,
) : LogLevelManager {
    private val logLevel = MutableStateFlow(defaultLevel)

    override fun override(level: LogLevel) {
        logLevel.update { level }
    }

    override fun restoreDefault() {
        override(defaultLevel)
    }

    override fun current(): LogLevel = logLevel.value
}
