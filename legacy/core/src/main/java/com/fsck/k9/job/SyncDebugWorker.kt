package com.fsck.k9.job

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eygraber.uri.Uri
import kotlinx.coroutines.CancellationException
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.logging.DebugLogConfigurator
import net.thunderbird.core.logging.SyncDebugLogExporter
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.update

private const val TAG = "SyncDebugWorker"

class SyncDebugWorker(
    context: Context,
    val baseLogger: Logger,
    private val logExporter: SyncDebugLogExporter,
    private val debugLogConfigurator: DebugLogConfigurator,
    parameters: WorkerParameters,
    val generalSettingsManager: GeneralSettingsManager,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val result = try {
            val uriString = inputData.getString("exportUriString")
            if (uriString == null) {
                Result.failure()
            } else {
                logExporter.export(Uri.parse(uriString))
                Result.success()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            baseLogger.error(tag = TAG, message = { "Failed to export log" }, throwable = e)
            Result.failure()
        } finally {
            debugLogConfigurator.updateSyncLogging(false)
            generalSettingsManager.update { settings ->
                settings.copy(debugging = settings.debugging.copy(isSyncLoggingEnabled = false))
            }
        }

        return result
    }
}
