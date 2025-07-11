package net.thunderbird.core.logging.legacy

import androidx.annotation.Discouraged
import net.thunderbird.core.logging.LogMessage
import net.thunderbird.core.logging.LogTag
import net.thunderbird.core.logging.Logger

/**
 * A static logging utility that implements [net.thunderbird.core.logging.Logger] and delegates to a [net.thunderbird.core.logging.Logger] implementation.
 *
 * You can initialize it in your application startup code, for example:
 *
 * ```kotlin
 * import net.thunderbird.core.logging.Log
 * import net.thunderbird.core.logging.DefaultLogger // or any other Logger implementation
 * fun main() {
 *     val sink: LogSink = // Your LogSink implementation
 *     val logger: Logger = DefaultLogger(sink)
 *
 *     Log.logger = logger
 *     Log.i("Application started")
 *     // Your application code here
 *  }
 * ```
 */
@Discouraged(
    message = "Use a net.thunderbird.core.logging.Logger instance via dependency injection instead. " +
        "This class will be removed in a future release.",
)
object Log : Logger {

    lateinit var logger: Logger

    override fun verbose(
        tag: LogTag?,
        throwable: Throwable?,
        message: () -> LogMessage,
    ) {
        logger.verbose(
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
        logger.debug(
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
        logger.info(
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
        logger.warn(
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
        logger.error(
            tag = tag,
            throwable = throwable,
            message = message,
        )
    }

    // Legacy Logger implementation

    @JvmStatic
    fun v(message: String?, vararg args: Any?) {
        logger.verbose(message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun v(t: Throwable?, message: String?, vararg args: Any?) {
        logger.verbose(message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun d(message: String?, vararg args: Any?) {
        logger.debug(message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun d(t: Throwable?, message: String?, vararg args: Any?) {
        logger.debug(message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun i(message: String?, vararg args: Any?) {
        logger.info(message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun i(t: Throwable?, message: String?, vararg args: Any?) {
        logger.info(message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun w(message: String?, vararg args: Any?) {
        logger.warn(message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun w(t: Throwable?, message: String?, vararg args: Any?) {
        logger.warn(message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun e(message: String?, vararg args: Any?) {
        logger.error(message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        logger.error(message = { formatMessage(message, args) }, throwable = t)
    }

    private fun formatMessage(message: String?, args: Array<out Any?>): String {
        return if (message == null) {
            ""
        } else if (args.isEmpty()) {
            message
        } else {
            try {
                String.format(message, *args)
            } catch (e: Exception) {
                "$message (Error formatting message: $e, args: ${args.joinToString()})"
            }
        }
    }
}
