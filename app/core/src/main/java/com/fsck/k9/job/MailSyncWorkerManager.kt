package com.fsck.k9.job

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fsck.k9.Account
import java.util.concurrent.TimeUnit
import timber.log.Timber

class MailSyncWorkerManager(private val workManager: WorkManager) {

    fun scheduleMailSync(account: Account) {
        getSyncIntervalInMinutesIfEnabled(account)?.let { syncInterval ->
            Timber.v("Scheduling mail sync worker for %s", account.description)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresStorageNotLow(true)
                    .build()

            val data = workDataOf(MailSyncWorker.EXTRA_ACCOUNT_UUID to account.uuid)

            val mailSyncRequest = PeriodicWorkRequestBuilder<MailSyncWorker>(syncInterval, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .addTag(MAIL_SYNC_TAG)
                    .build()

            val uniqueWorkName = createUniqueWorkName(account.uuid)
            workManager.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.REPLACE, mailSyncRequest)
        }
    }

    private fun getSyncIntervalInMinutesIfEnabled(account: Account): Long? {
        val intervalMinutes = account.automaticCheckIntervalMinutes
        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return intervalMinutes.toLong()
    }

    private fun createUniqueWorkName(accountUuid: String): String {
        return "$MAIL_SYNC_TAG:$accountUuid"
    }

    companion object {
        const val MAIL_SYNC_TAG = "MailSync"
    }
}
