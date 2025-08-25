package net.thunderbird.core.logging

/**
 * A sink that receives and processes log events.
 *
 * A `LogSink` determines whether to handle a log event based on its log level.
 * Log events with a level lower than the sink's configured level will be ignored.
 */
interface LogSink {

    /**
     * The minimum log level this sink will process.
     * Log events with a lower priority than this level will be ignored.
     */
    val level: LogLevel

    /**
     * Checks whether the sink is enabled for the given log level.
     *
     * @param level The log level to check.
     * @return `true` if this sink will process log events at this level or higher.
     */
    fun canLog(level: LogLevel): Boolean {
        return this.level <= level
    }

    /**
     * Logs a [LogEvent].
     *
     * @param event The [LogEvent] to log.
     */
    fun log(
        event: LogEvent,
    )
}
