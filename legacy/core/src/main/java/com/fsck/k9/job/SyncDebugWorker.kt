package com.fsck.k9.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fsck.k9.K9
import java.io.IOException
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink

class SyncDebugWorker(
    context: Context,
    val baseLogger: Logger,
    val fileLogSink: FileLogSink,
    val syncDebugCompositeSink: CompositeLogSink,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        try {
            fileLogSink.export(inputData.getString("exportUriString").toString())
        } catch (e: IOException) {
            baseLogger.error(message = { "Failed to export log" }, throwable = e)
            return Result.failure()
        }
        syncDebugCompositeSink.manager.remove(fileLogSink)
        K9.isSyncLoggingEnabled = false
        K9.saveSettingsAsync()
        return Result.success()
    }
}
