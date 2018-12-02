package com.fsck.k9.job

import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import timber.log.Timber


class MailSyncJob(
        private val messagingController: MessagingController,
        private val preferences: Preferences
) : K9Job() {

    // region K9Job methods

    override fun onRunJob(params: Params): Result {

        params.extras.getString(K9Job.EXTRA_KEY_ACCOUNT_UUID, null)?.let { accountUuid ->

            preferences.getAccount(accountUuid)?.let { account ->
                messagingController.checkMail(
                        context,
                        account,
                        false,
                        false,
                        null
                )
            }
        }

        return Result.SUCCESS
    }

    override fun scheduleJob(account: Account) {

        getSyncIntervalInMillisecondsIfEnabled(account)?.let { syncInterval ->

            Timber.v("scheduling mail sync job for ${account.description}")

            val extras = PersistableBundleCompat()
            extras.putString(K9Job.EXTRA_KEY_ACCOUNT_UUID, account.uuid)

            val jobRequest = JobRequest.Builder(MailSyncJob.TAG)
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

    private fun getSyncIntervalInMillisecondsIfEnabled(account: Account): Long? {
        val intervalMinutes = account.automaticCheckIntervalMinutes

        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return (intervalMinutes * 60 * 1000).toLong()
    }

    companion object {
        const val TAG: String = "MailSyncJob"
    }

}