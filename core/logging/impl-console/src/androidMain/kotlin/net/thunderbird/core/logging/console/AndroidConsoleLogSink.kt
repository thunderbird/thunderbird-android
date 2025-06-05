package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink
import timber.log.Timber

internal class AndroidConsoleLogSink(
    override val level: LogLevel,
) : LogSink {

    override fun log(event: LogEvent) {
        val timber = event.tag?.let { Timber.tag(it) } ?: Timber

        when (event.level) {
            LogLevel.VERBOSE -> timber.v(event.throwable, event.message)
            LogLevel.DEBUG -> timber.d(event.throwable, event.message)
            LogLevel.INFO -> timber.i(event.throwable, event.message)
            LogLevel.WARN -> timber.w(event.throwable, event.message)
            LogLevel.ERROR -> timber.e(event.throwable, event.message)
        }
    }
}
