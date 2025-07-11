package net.thunderbird.core.logging

/**
 * Represents the different levels of logging used to filter log messages.
 *
 * The log levels are ordered by priority, where a lower number indicates a more verbose level.
 * - [VERBOSE]: Most detailed log level, including all messages.
 * - [DEBUG]: Detailed information, typically useful for diagnosing problems.
 * - [INFO]: General information about the application state.
 * - [WARN]: Indicates something unexpected but not necessarily an error.
 * - [ERROR]: Indicates a failure or critical issue.
 *
 * Each log level has a priority, the higher the priority, the more important the log message is.
 *
 * @param priority The priority of the log level, where a lower priority indicates a more verbose level.
 */
enum class LogLevel(
    val priority: Int,
) {
    /**
     * Verbose log level — most detailed log level, including all messages.
     */
    VERBOSE(1),

    /**
     * Debug log level — detailed information, typically useful for diagnosing problems.
     */
    DEBUG(2),

    /**
     * Informational log level — general information about the application state.
     */
    INFO(3),

    /**
     * Warning log level — indicates something unexpected but not necessarily an error.
     */
    WARN(4),

    /**
     * Error log level — indicates a failure or critical issue.
     */
    ERROR(5),
}
