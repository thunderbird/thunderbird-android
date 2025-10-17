package com.fsck.k9.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eygraber.uri.toKmpUri
import java.io.IOException
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update

class SyncDebugWorker(
    context: Context,
    val baseLogger: Logger,
    val fileLogSink: FileLogSink,
    val syncDebugCompositeSink: CompositeLogSink,
    parameters: WorkerParameters,
    val generalSettingsManager: GeneralSettingsManager,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val result = try {
            val uriString = inputData.getString("exportUriString")
            if (uriString == null) {
                Result.failure()
            } else {
                fileLogSink.export(uriString.toKmpUri())
                Result.success()
            }
        } catch (e: IOException) {
            baseLogger.error(message = { "Failed to export log" }, throwable = e)
            Result.failure()
        }

        syncDebugCompositeSink.manager.remove(fileLogSink)
        generalSettingsManager.update { settings ->
            settings.copy(debugging = settings.debugging.copy(isSyncLoggingEnabled = false))
        }

        return result
    }
}
