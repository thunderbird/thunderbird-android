package net.thunderbird.core.logging.legacy

import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink
import timber.log.Timber
import timber.log.Timber.DebugTree

// TODO: Implementation https://github.com/thunderbird/thunderbird-android/issues/9573
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
