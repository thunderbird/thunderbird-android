package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import timber.log.Timber

actual fun ConsoleLogSink(level: LogLevel): ConsoleLogSink = AndroidConsoleLogSink(level)

private class AndroidConsoleLogSink(
    override val level: LogLevel,
) : ConsoleLogSink {

    override fun log(event: LogEvent) {
        val timber = event.tag
            ?.let { Timber.tag(it) }
            ?: Timber.tag(event.composeTag(ignoredClasses = IGNORE_CLASSES) ?: this::class.java.name)

        when (event.level) {
            LogLevel.VERBOSE -> timber.v(event.throwable, event.message)
            LogLevel.DEBUG -> timber.d(event.throwable, event.message)
            LogLevel.INFO -> timber.i(event.throwable, event.message)
            LogLevel.WARN -> timber.w(event.throwable, event.message)
            LogLevel.ERROR -> timber.e(event.throwable, event.message)
        }
    }

    companion object {
        private val IGNORE_CLASSES = setOf(
            Timber::class.java.name,
            Timber.Forest::class.java.name,
            Timber.Tree::class.java.name,
            Timber.DebugTree::class.java.name,
            AndroidConsoleLogSink::class.java.name,
            // Add other classes to ignore if needed
        )
    }
}
