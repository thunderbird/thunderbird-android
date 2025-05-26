package net.thunderbird.core.logging.console

import java.util.regex.Pattern
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

internal class JvmConsoleLogSink(
    level: LogLevel,
) : BaseConsoleLogSink(level) {

    override fun logWithTag(event: LogEvent, tag: String?) {
        println("[$level] ${composeMessage(event, tag)}")
        event.throwable?.printStackTrace()
    }

    private fun composeMessage(event: LogEvent, tag: String?): String {
        return if (tag != null) {
            "[$tag] ${event.message}"
        } else {
            event.message
        }
    }

    override fun getAnonymousClassPattern(): Pattern {
        return ANONYMOUS_CLASS
    }

    override fun getIgnoreClasses(): Set<String> {
        return IGNORE_CLASSES
    }

    companion object {
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")

        private val IGNORE_CLASSES = setOf(
            JvmConsoleLogSink::class.java.name,
            BaseConsoleLogSink::class.java.name,
            // Add other classes to ignore if needed
        )
    }
}
