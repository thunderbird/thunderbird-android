package com.fsck.k9.job

import androidx.work.Configuration
import androidx.work.WorkerFactory

class WorkManagerConfigurationProvider(private val workerFactory: WorkerFactory) {
    fun getConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
