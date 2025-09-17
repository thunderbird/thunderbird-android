package net.thunderbird.core.logging.config

import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink
import timber.log.Timber

class DebugLogConfigurator(
    private val syncDebugCompositeSink: CompositeLogSink,
    private val syncDebugFileLogSink: FileLogSink,
) {
    fun updateLoggingStatus(isDebugLoggingEnabled: Boolean) {
        Timber.Forest.uprootAll()
        if (isDebugLoggingEnabled) {
            Timber.Forest.plant(Timber.DebugTree())
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
