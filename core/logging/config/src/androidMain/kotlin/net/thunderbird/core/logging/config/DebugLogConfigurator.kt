package net.thunderbird.core.logging.config

import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink

class DebugLogConfigurator(
    private val syncDebugCompositeSink: CompositeLogSink,
    private val syncDebugFileLogSink: FileLogSink,
) {
    fun updateLoggingStatus(isDebugLoggingEnabled: Boolean) {
        syncDebugCompositeSink.manager.removeAll()
        if (isDebugLoggingEnabled) {
            syncDebugCompositeSink.manager.add(syncDebugCompositeSink)
            syncDebugCompositeSink.manager.add(syncDebugFileLogSink)
        }
    }

    fun updateSyncLogging(isSyncLoggingEnabled: Boolean) {
        if (isSyncLoggingEnabled) {
            syncDebugCompositeSink.manager.add(syncDebugFileLogSink)
        } else {
            syncDebugCompositeSink.manager.remove(syncDebugFileLogSink)
        }
    }
}
