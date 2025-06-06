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
        return if (event.tag != null) {
            "[${event.tag}] ${event.message}"
        } else {
            event.message
        }
    }
}
