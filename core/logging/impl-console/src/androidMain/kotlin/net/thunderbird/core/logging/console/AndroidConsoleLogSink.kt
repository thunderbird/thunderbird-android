package net.thunderbird.core.logging.console

import android.os.Build
import java.util.regex.Pattern
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import timber.log.Timber

internal class AndroidConsoleLogSink(
    level: LogLevel,
) : BaseConsoleLogSink(level) {

    override fun logWithTag(event: LogEvent, tag: String?) {
        val timber = tag?.let { Timber.tag(it) } ?: Timber

        when (event.level) {
            LogLevel.VERBOSE -> timber.v(event.throwable, event.message)
            LogLevel.DEBUG -> timber.d(event.throwable, event.message)
            LogLevel.INFO -> timber.i(event.throwable, event.message)
            LogLevel.WARN -> timber.w(event.throwable, event.message)
            LogLevel.ERROR -> timber.e(event.throwable, event.message)
        }
    }

    override fun processTag(tag: String): String {
        // Truncate tags to MAX_TAG_LENGTH when API level is lower than 26
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tag
        } else {
            tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    override fun getAnonymousClassPattern(): Pattern {
        return ANONYMOUS_CLASS
    }

    override fun getIgnoreClasses(): Set<String> {
        return IGNORE_CLASSES
    }

    companion object {
        private const val MAX_TAG_LENGTH = 23
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

        private val IGNORE_CLASSES = setOf(
            Timber::class.java.name,
            Timber.Forest::class.java.name,
            Timber.Tree::class.java.name,
            Timber.DebugTree::class.java.name,
            AndroidConsoleLogSink::class.java.name,
            BaseConsoleLogSink::class.java.name,
            // Add other classes to ignore if needed
        )
    }
}
