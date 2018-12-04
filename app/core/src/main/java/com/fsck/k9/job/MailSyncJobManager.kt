package com.fsck.k9.job

import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber

class MailSyncJobManager(
        private val messagingController: MessagingController,
        private val preferences: Preferences
) {

    fun getJob() = MailSyncJob(messagingController, preferences)

    fun scheduleJob(account: Account) {

        getSyncIntervalInMillisecondsIfEnabled(account)?.let { syncInterval ->

            Timber.v("scheduling mail sync job for ${account.description}")

            val extras = PersistableBundleCompat()
            extras.putString(K9JobManager.EXTRA_KEY_ACCOUNT_UUID, account.uuid)

            val jobRequest = JobRequest.Builder(MailSyncJob.TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .setPeriodic(syncInterval)
                    .setExtras(extras)
                    .setRequirementsEnforced(true)
                    .build()

            jobRequest.schedule()
        }
    }

    private fun getSyncIntervalInMillisecondsIfEnabled(account: Account): Long? {
        val intervalMinutes = account.automaticCheckIntervalMinutes

        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return (intervalMinutes * 60 * 1000).toLong()
    }

}