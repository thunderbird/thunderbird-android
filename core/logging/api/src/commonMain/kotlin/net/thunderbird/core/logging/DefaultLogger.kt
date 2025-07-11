package net.thunderbird.core.logging

import kotlinx.datetime.Clock

/**
 * Default implementation of [Logger] that logs messages to a [LogSink].
 *
 * @param sink The [LogSink] to which log events will be sent.
 * @param clock The [Clock] used to get the current time for log events. Defaults to the system clock.
 */
class DefaultLogger(
    private val sink: LogSink,
    private val clock: Clock = Clock.System,
) : Logger {

    private fun log(
        level: LogLevel,
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    ) {
        sink.let { currentSink ->
            if (currentSink.canLog(level)) {
                currentSink.log(
                    event = LogEvent(
                        level = level,
                        tag = tag,
                        message = message(),
                        throwable = throwable,
                        timestamp = clock.now().toEpochMilliseconds(),
                    ),
                )
            }
        }
    }

    override fun verbose(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        log(
            level = LogLevel.VERBOSE,
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun debug(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        log(
            level = LogLevel.DEBUG,
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun info(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        log(
            level = LogLevel.INFO,
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun warn(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        log(
            level = LogLevel.WARN,
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    override fun error(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        log(
            level = LogLevel.ERROR,
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }
}
