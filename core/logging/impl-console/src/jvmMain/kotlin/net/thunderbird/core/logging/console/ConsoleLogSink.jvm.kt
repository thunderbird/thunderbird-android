package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

actual fun ConsoleLogSink(level: LogLevel): ConsoleLogSink = JvmConsoleLogSink(level)

private class JvmConsoleLogSink(
    override val level: LogLevel,
) : ConsoleLogSink {

    override fun log(event: LogEvent) {
        println("[$level] ${composeMessage(event)}")
        event.throwable?.printStackTrace()
    }

    private fun composeMessage(event: LogEvent): String {
        val tag = event.tag ?: event.composeTag(ignoredClasses = IGNORE_CLASSES)
        return if (tag != null) {
            "[$tag] ${event.message}"
        } else {
            event.message
        }
    }

    companion object {
        private val IGNORE_CLASSES = setOf(
            JvmConsoleLogSink::class.java.name,
            // Add other classes to ignore if needed
        )
    }
}
