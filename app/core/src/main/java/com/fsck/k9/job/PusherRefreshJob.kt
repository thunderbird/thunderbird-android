package com.fsck.k9.job

import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber


class PusherRefreshJob(
        private val messagingController: MessagingController,
        private val preferences: Preferences
) : K9Job() {

    // region K9Job methods

    override fun onRunJob(params: Params): Result {
        params.extras.getString(K9Job.EXTRA_KEY_ACCOUNT_UUID, null)?.let { accountUuid ->

            preferences.getAccount(accountUuid)?.let { account ->
                try {
                    refreshPushers(account)
                } catch (e: Exception) {
                    Timber.e(e, "Exception while refreshing pushers")
                    return Result.RESCHEDULE
                }
            }
        }

        return Result.SUCCESS
    }

    override fun scheduleJob(account: Account) {

        getPushIntervalInMillisecondsIfEnabled(account)?.let { syncInterval ->

            Timber.v("scheduling pusher refresh job for ${account.description}")

            val extras = PersistableBundleCompat()
            extras.putString(K9Job.EXTRA_KEY_ACCOUNT_UUID, account.uuid)

            val jobRequest = JobRequest.Builder(TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .setPeriodic(syncInterval)
                    .setExtras(extras)
                    .setRequirementsEnforced(true)
                    .build()

            jobRequest.scheduleAsync()
        }

    }

    // endregion

    private fun refreshPushers(account: Account) {

        // Setup pushing
        var pushing = false
        Timber.i("Setting up pushers for account %s", account.description)

        if (account.isEnabled && account.isAvailable(context)) {
            pushing = MessagingController.getInstance(context).setupPushing(account)
        }

        if (!pushing) {
            return
        }

        // Refresh pushers
        Timber.i("Refreshing pusher for ${account.description}")

        messagingController.getPusher(account)?.refresh()

        // Whenever we refresh our pushers, send any unsent messages
        Timber.d("trying to send mail in all folders!")
        messagingController.sendPendingMessages(null)

    }

    private fun getPushIntervalInMillisecondsIfEnabled(account: Account): Long? {
        val intervalMinutes = account.idleRefreshMinutes

        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return (intervalMinutes * 60 * 1000).toLong()
    }

    companion object {
        const val TAG: String = "PusherRefreshJob"
    }

}