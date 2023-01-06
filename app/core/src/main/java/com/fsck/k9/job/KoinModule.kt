package com.fsck.k9.job

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import org.koin.dsl.module

val jobModule = module {
    single { WorkManagerConfigurationProvider(workerFactory = get()) }
    single<WorkerFactory> { K9WorkerFactory(get(), get()) }
    single { WorkManager.getInstance(get<Context>()) }
    single { K9JobManager(get(), get(), get()) }
    factory { MailSyncWorkerManager(workManager = get(), clock = get()) }
}
