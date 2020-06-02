package com.fsck.k9.job

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory

class WorkManagerProvider(private val context: Context, private val workerFactory: WorkerFactory) {
    fun getWorkManager(): WorkManager {
        val configuration = Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()

        WorkManager.initialize(context, configuration)

        return WorkManager.getInstance(context)
    }
}
