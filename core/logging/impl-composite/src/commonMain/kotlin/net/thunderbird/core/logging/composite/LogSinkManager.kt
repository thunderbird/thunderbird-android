package net.thunderbird.core.logging.composite

import net.thunderbird.core.logging.LogSink

/**
 * LogSinkManager is responsible for managing a collection of [LogSink] instances.
 */
interface LogSinkManager {

    /**
     * Retrieves all [LogSink] instances managed by this manager.
     *
     * @return A list of all sinks.
     */
    fun getAll(): List<LogSink>

    /**
     * Adds a [LogSink] to the manager.
     *
     * @param sink The [LogSink] to add.
     */
    fun add(sink: LogSink)

    /**
     * Adds multiple [LogSink] instances to the manager.
     *
     * @param sinks The list of [LogSink] to add.
     */
    fun addAll(sinks: List<LogSink>)

    /**
     * Removes a [LogSink] from the manager.
     *
     * @param sink The [LogSink] to remove.
     */
    fun remove(sink: LogSink)

    /**
     * Removes all [LogSink] instances from the manager.
     */
    fun removeAll()
}
