package com.fsck.k9.job

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import net.thunderbird.core.logging.composite.CompositeLogSink
import net.thunderbird.core.logging.file.FileLogSink

class SyncDebugWorker(
    context: Context,
    val fileLogSink: FileLogSink,
    val syncDebugCompositeSink: CompositeLogSink,
    parameters: WorkerParameters,
) : Worker(context, parameters) {
    override fun doWork(): Result {
        try {
            fileLogSink.export(inputData.getString("exportUriString").toString())
        } catch (_: IOException) {
            return Result.failure()
        }
        syncDebugCompositeSink.manager.remove(fileLogSink)
        return Result.success()
    }
}
