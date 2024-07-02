package com.fsck.k9.job

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fsck.k9.Account
import com.fsck.k9.K9
import java.util.concurrent.TimeUnit
import kotlinx.datetime.Clock
import timber.log.Timber

class MailSyncWorkerManager(
    private val workManager: WorkManager,
    val clock: Clock,
) {

    fun cancelMailSync(account: Account) {
        Timber.v("Canceling mail sync worker for %s", account)
        val uniqueWorkName = createUniqueWorkName(account.uuid)
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    fun scheduleMailSync(account: Account) {
        if (isNeverSyncInBackground()) return

        getSyncIntervalIfEnabled(account)?.let { syncIntervalMinutes ->
            Timber.v("Scheduling mail sync worker for %s", account)
            Timber.v("  sync interval: %d minutes", syncIntervalMinutes)

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()

            val lastSyncTime = account.lastSyncTime
            Timber.v("  last sync time: %tc", lastSyncTime)

            val initialDelay = calculateInitialDelay(lastSyncTime, syncIntervalMinutes)
            Timber.v("  initial delay: %d ms", initialDelay)

            val data = workDataOf(MailSyncWorker.EXTRA_ACCOUNT_UUID to account.uuid)

            val mailSyncRequest = PeriodicWorkRequestBuilder<MailSyncWorker>(syncIntervalMinutes, TimeUnit.MINUTES)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(MAIL_SYNC_TAG)
                .build()

            val uniqueWorkName = createUniqueWorkName(account.uuid)
            workManager.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.REPLACE, mailSyncRequest)
        }
    }

    private fun isNeverSyncInBackground() = K9.backgroundOps == K9.BACKGROUND_OPS.NEVER

    private fun getSyncIntervalIfEnabled(account: Account): Long? {
        val intervalMinutes = account.automaticCheckIntervalMinutes
        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return intervalMinutes.toLong()
    }

    private fun calculateInitialDelay(lastSyncTime: Long, syncIntervalMinutes: Long): Long {
        val now = clock.now().toEpochMilliseconds()
        val nextSyncTime = lastSyncTime + (syncIntervalMinutes * 60L * 1000L)

        return if (lastSyncTime > now || nextSyncTime <= now) {
            0L
        } else {
            nextSyncTime - now
        }
    }

    private fun createUniqueWorkName(accountUuid: String): String {
        return "$MAIL_SYNC_TAG:$accountUuid"
    }

    companion object {
        const val MAIL_SYNC_TAG = "MailSync"
        private const val INITIAL_BACKOFF_DELAY_MINUTES = 5L
    }
}
