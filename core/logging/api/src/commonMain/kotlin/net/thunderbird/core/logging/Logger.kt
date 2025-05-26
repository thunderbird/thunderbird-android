package net.thunderbird.core.logging

/**
 * A logging interface that provides methods for logging messages at specific log levels.
 */
interface Logger {
    /**
     * Logs a message at the verbose log level.
     *
     * @param tag An optional [LogTag] to categorize the log message.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    fun verbose(
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the debug log level.
     *
     * @param tag An optional [LogTag] to categorize the log message.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    fun debug(
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the info log level.
     *
     * @param tag An optional [LogTag] to categorize the log message.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    fun info(
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the warn log level.
     *
     * @param tag An optional [LogTag] to categorize the log message.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    fun warn(
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )

    /**
     * Logs a message at the error log level.
     *
     * @param tag An optional [LogTag] to categorize the log message.
     * @param throwable An optional throwable to log.
     * @param message Lambda that returns the [LogMessage] to log.
     */
    fun error(
        tag: LogTag? = null,
        throwable: Throwable? = null,
        message: () -> LogMessage,
    )
}
