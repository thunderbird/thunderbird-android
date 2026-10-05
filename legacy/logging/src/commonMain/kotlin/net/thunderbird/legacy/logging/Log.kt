package net.thunderbird.legacy.logging

import androidx.annotation.Discouraged
import net.thunderbird.components.core.logging.LogMessage
import net.thunderbird.components.core.logging.LogTag
import net.thunderbird.components.core.logging.Logger

private const val TAG = "Log"

/**
 * Legacy static facade over a [Logger].
 *
 * New code should receive a [Logger] through dependency injection. This facade exists only for Java and Kotlin
 * callers that still use the historic static logging methods.
 *
 * You can initialize it in your application startup code, for example:
 *
 * ```kotlin
 * import net.thunderbird.legacy.logging.Log
 * import net.thunderbird.components.core.logging.DefaultLogger // or any other Logger implementation
 * fun main() {
 *     val sink: LogSink = // Your LogSink implementation
 *     val logger: Logger = DefaultLogger(sink)
 *
 *     Log.logger = logger
 *     Log.i(TAG, "Application started")
 *     // Your application code here
 *  }
 * ```
 */
@Discouraged(
    message = "Use a net.thunderbird.components.core.logging.Logger instance via dependency injection instead. " +
        "This class will be removed in a future release.",
)
@Suppress("TooManyFunctions")
object Log : Logger {

    lateinit var logger: Logger

    override fun verbose(
        tag: LogTag,
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
        tag: LogTag,
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
        tag: LogTag,
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
        tag: LogTag,
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
        tag: LogTag,
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
    fun v(tag: String, message: String?, vararg args: Any?) {
        logger.verbose(tag = tag, message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun v(tag: String, t: Throwable?, message: String?, vararg args: Any?) {
        logger.verbose(tag = tag, message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun d(tag: String, message: String?, vararg args: Any?) {
        logger.debug(tag = tag, message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun d(tag: String, t: Throwable?, message: String?, vararg args: Any?) {
        logger.debug(tag = tag, message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun i(tag: String, message: String?, vararg args: Any?) {
        logger.info(tag = tag, message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun i(tag: String, t: Throwable?, message: String?, vararg args: Any?) {
        logger.info(tag = tag, message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun w(tag: String, message: String?, vararg args: Any?) {
        logger.warn(tag = tag, message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun w(tag: String, t: Throwable?, message: String?, vararg args: Any?) {
        logger.warn(tag = tag, message = { formatMessage(message, args) }, throwable = t)
    }

    @JvmStatic
    fun e(tag: String, message: String?, vararg args: Any?) {
        logger.error(tag = tag, message = { formatMessage(message, args) })
    }

    @JvmStatic
    fun e(tag: String, t: Throwable?, message: String?, vararg args: Any?) {
        logger.error(tag = tag, message = { formatMessage(message, args) }, throwable = t)
    }

    @Suppress("SpreadOperator", "TooGenericExceptionCaught")
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
