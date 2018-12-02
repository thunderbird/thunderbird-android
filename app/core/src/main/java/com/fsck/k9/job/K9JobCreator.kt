package com.fsck.k9.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class K9JobCreator(
        val mailSyncJob: MailSyncJob,
        val pusherRefreshJob: PusherRefreshJob
) : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            MailSyncJob.TAG -> mailSyncJob
            PusherRefreshJob.TAG -> pusherRefreshJob
            else -> null
        }
    }

}