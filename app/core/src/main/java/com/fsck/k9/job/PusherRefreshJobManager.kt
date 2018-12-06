package com.fsck.k9.job

import android.content.Context
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber

class PusherRefreshJobManager(
        private val context: Context,
        private val messagingController: MessagingController,
        private val preferences: Preferences
) {

    fun getJob() = PusherRefreshJob(messagingController, preferences)

    fun scheduleJob(account: Account) {

        if (!isPushEnabled(account)) {
            return
        }

        getPushIntervalInMillisecondsIfEnabled(account)?.let { syncInterval ->

            Timber.v("scheduling pusher refresh job for ${account.description}")

            val extras = PersistableBundleCompat()
            extras.putString(K9JobManager.EXTRA_KEY_ACCOUNT_UUID, account.uuid)

            val jobRequest = JobRequest.Builder(PusherRefreshJob.TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .setPeriodic(syncInterval)
                    .setExtras(extras)
                    .setRequirementsEnforced(true)
                    .build()

            jobRequest.scheduleAsync()
        }

    }

    private fun getPushIntervalInMillisecondsIfEnabled(account: Account): Long? {
        val intervalMinutes = account.idleRefreshMinutes

        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return (intervalMinutes * 60 * 1000).toLong()
    }

    private fun isPushEnabled(account: Account): Boolean {
        if (account.isEnabled && account.isAvailable(context)) {
            Timber.i("Setting up pushers for account %s", account.description)
            return messagingController.setupPushing(account)
        }

        return false
    }

}