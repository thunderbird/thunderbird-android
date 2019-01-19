package com.fsck.k9.job

import com.evernote.android.job.JobManager
import org.koin.dsl.module.applicationContext

val jobModule = applicationContext {
    bean { JobManager.create(get()) as JobManager }
    bean { K9JobManager(get(), get(), get(), get(), get()) }
    bean { K9JobCreator(get(), get()) }
    factory { MailSyncJobManager(get(), get()) }
    factory { PusherRefreshJobManager(get(), get(), get()) }
}
