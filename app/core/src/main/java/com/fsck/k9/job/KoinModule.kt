package com.fsck.k9.job

import androidx.work.WorkerFactory
import org.koin.dsl.module.applicationContext

val jobModule = applicationContext {
    bean { WorkManagerProvider(get(), get()) }
    bean<WorkerFactory> { K9WorkerFactory(get(), get()) }
    bean { get<WorkManagerProvider>().getWorkManager() }
    bean { K9JobManager(get(), get(), get()) }
    factory { MailSyncWorkerManager(get()) }
}
