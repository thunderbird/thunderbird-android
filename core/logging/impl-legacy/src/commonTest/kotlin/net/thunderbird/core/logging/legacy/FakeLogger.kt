package net.thunderbird.core.logging.legacy

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.Logger

class FakeLogger : Logger {
    val events = mutableListOf<LogEvent>()

    override fun verbose(tag: String?, message: String, throwable: Throwable?) {
        events.add(LogEvent(LogLevel.VERBOSE, tag, message, throwable, TIMESTAMP))
    }

    override fun debug(tag: String?, message: String, throwable: Throwable?) {
        events.add(LogEvent(LogLevel.DEBUG, tag, message, throwable, TIMESTAMP))
    }

    override fun info(tag: String?, message: String, throwable: Throwable?) {
        events.add(LogEvent(LogLevel.INFO, tag, message, throwable, TIMESTAMP))
    }

    override fun warn(tag: String?, message: String, throwable: Throwable?) {
        events.add(LogEvent(LogLevel.WARN, tag, message, throwable, TIMESTAMP))
    }

    override fun error(tag: String?, message: String, throwable: Throwable?) {
        events.add(LogEvent(LogLevel.ERROR, tag, message, throwable, TIMESTAMP))
    }

    private companion object {
        const val TIMESTAMP = 0L
    }
}
