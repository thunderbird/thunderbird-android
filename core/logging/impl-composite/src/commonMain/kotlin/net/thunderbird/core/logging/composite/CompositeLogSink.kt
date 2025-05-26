package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * A [LogSink] that aggregates multiple [LogSink] and forwards log events to them.
 *
 * This [LogSink] is useful when you want to log messages to multiple destinations
 * (e.g., console, file, etc.) without having to manage each [LogSink] individually.
 *
 * It checks the log level of each event against its own level and forwards the event
 * to all managed sinks that can handle the event's level.
 *
 * @param level The minimum log level this sink will process. Log events with a lower priority will be ignored.
 * @param manager The [LogSinkManager] that manages the collection of sinks.
 * @param sinks Initial list of [LogSink] to be managed by this composite sink.
 */
class CompositeLogSink(
    override val level: LogLevel,
    private val manager: LogSinkManager = DefaultLogSinkManager(),
    sinks: List<LogSink> = emptyList(),
) : LogSink {

    init {
        manager.addAll(sinks)
    }

    override fun log(event: LogEvent) {
        if (canLog(event.level)) {
            manager.getAll().forEach { sink ->
                if (sink.canLog(event.level)) {
                    sink.log(event)
                }
            }
        }
    }
}
