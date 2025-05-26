package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

/**
 * Default implementation of [LogSinkManager] that manages a collection of [LogSink] instances.
 */
class DefaultLogSinkManager : LogSinkManager {
    private val sinks: MutableList<LogSink> = mutableListOf()

    override fun getAll(): List<LogSink> {
        return sinks.toList()
    }

    override fun addAll(sinks: List<LogSink>) {
        sinks.forEach {
            add(it)
        }
    }

    override fun add(sink: LogSink) {
        if (sink !in sinks) {
            sinks.add(sink)
        }
    }

    override fun remove(sink: LogSink) {
        if (sink in sinks) {
            sinks.remove(sink)
        }
    }

    override fun removeAll() {
        sinks.clear()
    }
}
