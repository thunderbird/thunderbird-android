package com.fsck.k9.job

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log

class K9JobManager(
    private val workManager: WorkManager,
    private val accountManager: AccountManager,
    private val mailSyncWorkerManager: MailSyncWorkerManager,
    private val syncDebugFileLogManager: FileLogLimitWorkManager,
) {
    fun scheduleDebugLogLimit(contentUriString: String): Flow<WorkInfo?> {
        return syncDebugFileLogManager.scheduleFileLogTimeLimit(contentUriString)
    }

    fun cancelDebugLogLimit() {
        syncDebugFileLogManager.cancelFileLogTimeLimit()
    }

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
