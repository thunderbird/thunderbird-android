package com.fsck.k9.job

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.fsck.k9.Account
import com.fsck.k9.Clock
import com.fsck.k9.K9
import java.time.Duration
import timber.log.Timber

class MailSyncWorkerManager(private val workManager: WorkManager, val clock: Clock) {

    fun cancelMailSync(account: Account) {
        Timber.v("Canceling mail sync worker for %s", account.description)
        val uniqueWorkName = createUniqueWorkName(account.uuid)
        workManager.cancelUniqueWork(uniqueWorkName)
    }

    fun scheduleMailSync(account: Account) {
        if (isNeverSyncInBackground()) return

        getSyncIntervalIfEnabled(account)?.let { syncInterval ->
            Timber.v("Scheduling mail sync worker for %s", account.description)
            Timber.v("  sync interval: %d minutes", syncInterval.toMinutes())

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresStorageNotLow(true)
                .build()

            val lastSyncTime = account.lastSyncTime
            Timber.v("  last sync time: %tc", lastSyncTime)

            val initialDelay = calculateInitialDelay(lastSyncTime, syncInterval)
            Timber.v("  initial delay: %d minutes", initialDelay.toMinutes())

            val data = workDataOf(MailSyncWorker.EXTRA_ACCOUNT_UUID to account.uuid)

            val mailSyncRequest = PeriodicWorkRequestBuilder<MailSyncWorker>(syncInterval)
                .setInitialDelay(initialDelay)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_DELAY)
                .setConstraints(constraints)
                .setInputData(data)
                .addTag(MAIL_SYNC_TAG)
                .build()

            val uniqueWorkName = createUniqueWorkName(account.uuid)
            workManager.enqueueUniquePeriodicWork(uniqueWorkName, ExistingPeriodicWorkPolicy.REPLACE, mailSyncRequest)
        }
    }

    private fun isNeverSyncInBackground() = K9.backgroundOps == K9.BACKGROUND_OPS.NEVER

    private fun getSyncIntervalIfEnabled(account: Account): Duration? {
        val intervalMinutes = account.automaticCheckIntervalMinutes
        if (intervalMinutes <= Account.INTERVAL_MINUTES_NEVER) {
            return null
        }

        return Duration.ofMinutes(intervalMinutes.toLong())
    }

    private fun calculateInitialDelay(lastSyncTime: Long, syncInterval: Duration): Duration {
        val now = clock.time
        val nextSyncTime = lastSyncTime + syncInterval.toMillis()

        return if (lastSyncTime > now || nextSyncTime <= now) {
            Duration.ZERO
        } else {
            Duration.ofMillis(nextSyncTime - now)
        }
    }

    private fun createUniqueWorkName(accountUuid: String): String {
        return "$MAIL_SYNC_TAG:$accountUuid"
    }

    companion object {
        const val MAIL_SYNC_TAG = "MailSync"
        private val INITIAL_BACKOFF_DELAY = Duration.ofMinutes(5)
    }
}
