package com.fsck.k9.preferences

import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink
import timber.log.Timber
import timber.log.Timber.DebugTree

class DebugLogConfigurator(
    private val syncDebugCompositeSink: CompositeLogSink,
    private val syncDebugFileLogSink: FileLogSink,
) {
    fun updateLoggingStatus(isDebugLoggingEnabled: Boolean) {
        Timber.uprootAll()
        if (isDebugLoggingEnabled) {
            Timber.plant(DebugTree())
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
