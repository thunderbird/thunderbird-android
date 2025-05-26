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
}
