package com.fsck.k9.job

import androidx.work.WorkManager
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log

class K9JobManager(
    private val workManager: WorkManager,
    private val accountManager: AccountManager,
    private val mailSyncWorkerManager: MailSyncWorkerManager,
) {
    fun scheduleAllMailJobs() {
        Log.v("scheduling all jobs")
        scheduleMailSync()
    }

    fun scheduleMailSync(account: LegacyAccount) {
        mailSyncWorkerManager.cancelMailSync(account)
        mailSyncWorkerManager.scheduleMailSync(account)
    }

    private fun scheduleMailSync() {
        cancelAllMailSyncJobs()

        accountManager.getAccounts().forEach { account ->
            mailSyncWorkerManager.scheduleMailSync(account)
        }
    }

    private fun cancelAllMailSyncJobs() {
        Log.v("canceling mail sync job")
        workManager.cancelAllWorkByTag(MailSyncWorkerManager.MAIL_SYNC_TAG)
    }
}
