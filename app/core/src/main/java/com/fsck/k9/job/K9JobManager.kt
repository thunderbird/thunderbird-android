package com.fsck.k9.job

import com.evernote.android.job.JobManager
import com.fsck.k9.Preferences
import timber.log.Timber

class K9JobManager(
        jobCreator: K9JobCreator,
        private val jobManager: JobManager,
        private val preferences: Preferences,
        private val mailSyncJobManager: MailSyncJobManager,
        private val pusherRefreshJobManager: PusherRefreshJobManager
) {

    // It's recommended to initialize JobManager in Application onCreate()
    // I.e. by calling JobManager.create(this).addJobCreator(jobCreator)
    // Using this DI approach should provide a similar initialization
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
            mailSyncJobManager.scheduleJob(account)
        }
    }

    fun schedulePusherRefresh() {
        cancelAllPusherRefreshJobs()

        preferences.availableAccounts?.forEach { account ->
            pusherRefreshJobManager.scheduleJob(account)
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

    companion object {
        const val EXTRA_KEY_ACCOUNT_UUID = "param_key_account_uuid"
    }

}