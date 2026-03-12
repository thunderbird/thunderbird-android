package net.thunderbird.core.logging.testing

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger

/**
 * A test logger that captures all log events in a list.
 */
class TestLogger : Logger {

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

    /**
     * Outputs all collected log events to standard output.
     *
     * Each log event is formatted with its log level prefix (first letter uppercase followed by colon),
     * the log message, and optionally the stack trace of any associated throwable.
     * Multi-line messages are indented to align with the content after the log level prefix.
     * Stack traces are indented twice the length of the log level prefix.
     */
    fun dump() {
        events.forEach { event ->
            val composedLog = buildString {
                val logLevel = "${event.level.toString().take(1).uppercase()}: "
                append(logLevel)
                val firstLine = event.message.takeWhile { it != '\n' }
                val rest = event.message.drop(firstLine.length)
                append(firstLine)
                appendLine(rest.prependIndent(" ".repeat(logLevel.length)))

                event.throwable?.let {
                    appendLine(it.stackTraceToString().prependIndent(" ".repeat(logLevel.length * 2)))
                }
            }

            print(composedLog)
        }
    }

    companion object {
        const val TIMESTAMP = 1000L
    }
}
