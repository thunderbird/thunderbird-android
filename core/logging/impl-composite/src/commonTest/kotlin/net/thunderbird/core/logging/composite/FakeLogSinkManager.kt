package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

class FakeLogSinkManager(
    val sinks: MutableList<LogSink> = mutableListOf(),
) : LogSinkManager {

    override fun getAll(): List<LogSink> = sinks

    override fun add(sink: LogSink) = Unit

    override fun addAll(sinks: List<LogSink>) {
        this.sinks.addAll(sinks)
    }

    override fun remove(sink: LogSink) = Unit

    override fun removeAll() = Unit
}
