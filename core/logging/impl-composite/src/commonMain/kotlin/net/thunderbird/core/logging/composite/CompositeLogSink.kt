package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

/**
 * A [LogSink] that aggregates multiple [LogSink] and forwards log events to them.
 *
 * This [CompositeLogSink] is useful when you want to log messages to multiple destinations
 * (e.g., console, file, etc.) without having to manage each [LogSink] individually.
 *
 * It checks the log level of each event against its own level and forwards the event
 * to all managed sinks that can handle the event's level.
 *
 * @param level The minimum log level this sink will process. Log events with a lower priority will be ignored.
 * @param manager The [CompositeLogSinkManager] that manages the collection of sinks.
 */
interface CompositeLogSink : LogSink {
    val manager: CompositeLogSinkManager
}

/**
 * Creates a [CompositeLogSink] with the specified log level and manager.
 *
 * @param level The minimum [LogLevel] for messages to be logged.
 * @param manager The [CompositeLogSinkManager] that manages the collection of sinks.
 * @param sinks A list of [LogSink] instances to be managed by this composite sink.
 * @return A new instance of [CompositeLogSink].
 */
fun CompositeLogSink(
    level: LogLevel,
    manager: CompositeLogSinkManager = DefaultLogSinkManager(),
    sinks: List<LogSink> = emptyList(),
): CompositeLogSink {
    return DefaultCompositeLogSink(level, manager, sinks)
}
