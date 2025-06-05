package net.thunderbird.core.logging

class FakeLogSink(
    override val level: LogLevel = LogLevel.VERBOSE,
) : LogSink {

    val events = mutableListOf<LogEvent>()

    override fun log(event: LogEvent) {
        events.add(event)
    }
}
