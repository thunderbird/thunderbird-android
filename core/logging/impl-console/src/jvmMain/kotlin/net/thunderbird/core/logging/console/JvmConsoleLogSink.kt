package net.thunderbird.core.logging.console

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

internal class JvmConsoleLogSink(
    override val level: LogLevel,
) : LogSink {

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
