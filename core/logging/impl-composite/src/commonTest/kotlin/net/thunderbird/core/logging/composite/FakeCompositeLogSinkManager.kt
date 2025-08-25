package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

class FakeCompositeLogSinkManager(
    val sinks: MutableList<LogSink> = mutableListOf(),
) : CompositeLogSinkManager {

    override fun getAll(): List<LogSink> = sinks

    override fun add(sink: LogSink) = Unit

    override fun addAll(sinks: List<LogSink>) {
        this.sinks.addAll(sinks)
    }

    override fun remove(sink: LogSink) = Unit

    override fun removeAll() = Unit
}
