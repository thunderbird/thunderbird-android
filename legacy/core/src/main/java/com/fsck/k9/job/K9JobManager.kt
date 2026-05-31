package com.fsck.k9.job

import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.legacy.Log

class K9JobManager(
    private val workManager: WorkManager,
    private val accountManager: LegacyAccountDtoManager,
    private val mailSyncWorkerManager: MailSyncWorkerManager,
    private val attachmentCleanupWorkerManager: AttachmentCleanupWorkerManager,
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
        scheduleAttachmentCleanup()
    }

    fun scheduleMailSync(account: LegacyAccountDto) {
        mailSyncWorkerManager.cancelMailSync(account)
        mailSyncWorkerManager.scheduleMailSync(account)
    }

    fun scheduleAttachmentCleanup(account: LegacyAccountDto, runNow: Boolean = false) {
        attachmentCleanupWorkerManager.scheduleAttachmentCleanup(account)
        if (runNow) {
            attachmentCleanupWorkerManager.scheduleAttachmentCleanupNow(account)
        }
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

    private fun scheduleAttachmentCleanup() {
        attachmentCleanupWorkerManager.cancelAllAttachmentCleanup()

        accountManager.getAccounts().forEach { account ->
            attachmentCleanupWorkerManager.scheduleAttachmentCleanup(account)
        }
    }
}
