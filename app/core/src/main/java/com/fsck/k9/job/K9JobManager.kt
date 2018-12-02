package com.fsck.k9.job

import com.evernote.android.job.JobManager
import com.fsck.k9.Preferences
import timber.log.Timber

class K9JobManager(
        private val jobManager: JobManager,
        private val jobCreator: K9JobCreator,
        private val preferences: Preferences
) {

    init {
        jobManager.addJobCreator(jobCreator)
    }

    fun scheduleAllMailJobs() {
        Timber.v("scheduling all jobs")
        scheduleMailSync()
        schedulePusherRefresh()
    }

    fun scheduleMailSync() {
        cancelAllMailSyncJobs()

        preferences.availableAccounts?.forEach { account ->
            jobCreator.mailSyncJob.scheduleJob(account)
        }
    }

    fun schedulePusherRefresh() {
        cancelAllPusherRefreshJobs()

        preferences.availableAccounts?.forEach { account ->
            jobCreator.pusherRefreshJob.scheduleJob(account)
        }
    }

    fun cancelAllMailSyncJobs() {
        Timber.v("canceling mail sync job")
        jobManager.cancelAllForTag(MailSyncJob.TAG)
    }

    fun cancelAllPusherRefreshJobs() {
        Timber.v("canceling pusher refresh job")
        jobManager.cancelAllForTag(PusherRefreshJob.TAG)
    }

}