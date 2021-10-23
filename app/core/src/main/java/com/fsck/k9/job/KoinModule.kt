package com.fsck.k9.job

import androidx.work.WorkerFactory
import org.koin.dsl.module

val jobModule = module {
    single { WorkManagerProvider(get(), get()) }
    single<WorkerFactory> { K9WorkerFactory(get(), get()) }
    single { get<WorkManagerProvider>().getWorkManager() }
    single { K9JobManager(get(), get(), get()) }
    factory { MailSyncWorkerManager(workManager = get(), clock = get()) }
}
