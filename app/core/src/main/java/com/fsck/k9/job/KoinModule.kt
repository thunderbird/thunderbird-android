package com.fsck.k9.job

import com.evernote.android.job.JobManager
import org.koin.dsl.module.module

val jobModule = module {
    single { JobManager.create(get()) as JobManager }
    single { K9JobManager(get(), get(), get(), get(), get()) }
    single { K9JobCreator(get(), get()) }
    factory { MailSyncJobManager(get(), get()) }
    factory { PusherRefreshJobManager(get(), get(), get()) }
}
