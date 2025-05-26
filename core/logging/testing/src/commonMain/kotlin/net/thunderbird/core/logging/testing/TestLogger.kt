package net.thunderbird.core.logging.testing

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger

/**
 * A test logger that captures all log events in a list.
 */
class TestLogger() : Logger {

    val events: MutableList<LogEvent> = mutableListOf()

    override fun verbose(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun debug(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun info(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.INFO,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun warn(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.WARN,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    override fun error(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        events.add(
            LogEvent(
                level = LogLevel.ERROR,
                tag = tag,
                message = message(),
                throwable = throwable,
                timestamp = TIMESTAMP,
            ),
        )
    }

    companion object {
        const val TIMESTAMP = 1000L
    }
}
