package net.thunderbird.core.logging.internal

import net.thunderbird.components.core.logging.composite.CompositeLogSink
import net.thunderbird.components.core.logging.file.FileLogSink
import net.thunderbird.core.logging.DebugLogConfigurator

internal class DefaultDebugLogConfigurator(
    private val syncDebugCompositeSink: CompositeLogSink,
    private val syncDebugFileLogSink: FileLogSink,
) : DebugLogConfigurator {
    override fun updateSyncLogging(isSyncLoggingEnabled: Boolean) {
        if (isSyncLoggingEnabled) {
            syncDebugCompositeSink.manager.add(syncDebugFileLogSink)
        } else {
            syncDebugCompositeSink.manager.remove(syncDebugFileLogSink)
        }
    }
}
