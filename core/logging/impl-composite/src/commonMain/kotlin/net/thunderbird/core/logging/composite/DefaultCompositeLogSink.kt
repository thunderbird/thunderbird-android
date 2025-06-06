package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink

internal class DefaultCompositeLogSink(
    override val level: LogLevel,
    override val manager: CompositeLogSinkManager = DefaultLogSinkManager(),
    sinks: List<LogSink> = emptyList(),
) : CompositeLogSink {

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
