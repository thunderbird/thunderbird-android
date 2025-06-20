package net.thunderbird.core.logging.file

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

internal class JvmFileLogSink(
    override val level: LogLevel,
    fileName: String,
    fileLocation: String,
) : FileLogSink {

    override fun log(event: LogEvent) {
        println("[$level] ${composeMessage(event)}")
        event.throwable?.printStackTrace()
    }

    override fun export(uriString: String) {
        TODO("Not yet implemented")
    }

    override fun flushAndCloseBuffer() {
        TODO("Not yet implemented")
    }

    private fun composeMessage(event: LogEvent): String {
        return if (event.tag != null) {
            "[${event.tag}] ${event.message}"
        } else {
            event.message
        }
    }
}
