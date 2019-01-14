package com.fsck.k9.job

import com.evernote.android.job.JobManager
import org.koin.dsl.module.applicationContext

val jobModule = applicationContext {

    // Creating android-job JobManager fails with Robolectric tests
    // Let's avoid accessing JobManager during Robolectric tests only
    // See: https://github.com/evernote/android-job/issues/220
    if (!isRobolectricUnitTest()) {
        bean { JobManager.create(get()) as JobManager }
        bean { K9JobManager(get(), get(), get(), get(), get()) }
        bean { K9JobCreator(get(), get()) }
        factory { MailSyncJobManager(get(), get()) }
        factory { PusherRefreshJobManager(get(), get(), get()) }
    }
}

fun isRobolectricUnitTest(): Boolean {
    return try {
        Class.forName("org.robolectric.RobolectricTestRunner")
        true
    } catch (e: Exception) {
        false
    }
}
