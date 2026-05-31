package com.fsck.k9.job

import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.MIN_ATTACHMENT_CLEANUP_DAYS
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.core.logging.legacy.Log

class AttachmentCleanupWorkerManager(
    private val workManager: WorkManager,
) {
    fun scheduleAttachmentCleanup(account: LegacyAccountDto) {
        val uniqueWorkName = createUniqueWorkName(account.uuid)
        if (!account.isAttachmentCleanupEnabled()) {
            workManager.cancelUniqueWork(uniqueWorkName)
            workManager.cancelUniqueWork(createOneTimeUniqueWorkName(account.uuid))
            return
        }

        Log.v("Scheduling attachment cleanup worker for %s", account)

        val data = workDataOf(AttachmentCleanupWorker.EXTRA_ACCOUNT_UUID to account.uuid)
        val request = PeriodicWorkRequestBuilder<AttachmentCleanupWorker>(CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS)
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(ATTACHMENT_CLEANUP_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueWorkName,
            ExistingPeriodicWorkPolicy.REPLACE,
            request,
        )
    }

    fun scheduleAttachmentCleanupNow(account: LegacyAccountDto) {
        if (!account.isAttachmentCleanupEnabled()) return

        val data = workDataOf(AttachmentCleanupWorker.EXTRA_ACCOUNT_UUID to account.uuid)
        val request = OneTimeWorkRequestBuilder<AttachmentCleanupWorker>()
            .setInputData(data)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INITIAL_BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(ATTACHMENT_CLEANUP_TAG)
            .build()

        workManager.enqueueUniqueWork(
            createOneTimeUniqueWorkName(account.uuid),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAllAttachmentCleanup() {
        workManager.cancelAllWorkByTag(ATTACHMENT_CLEANUP_TAG)
    }

    private fun LegacyAccountDto.isAttachmentCleanupEnabled(): Boolean {
        return attachmentCleanupDays >= MIN_ATTACHMENT_CLEANUP_DAYS && incomingServerSettings.type == Protocols.IMAP
    }

    private fun createUniqueWorkName(accountUuid: String): String {
        return "$ATTACHMENT_CLEANUP_TAG:$accountUuid"
    }

    private fun createOneTimeUniqueWorkName(accountUuid: String): String {
        return "$ATTACHMENT_CLEANUP_NOW_TAG:$accountUuid"
    }

    companion object {
        const val ATTACHMENT_CLEANUP_TAG = "AttachmentCleanup"
        private const val ATTACHMENT_CLEANUP_NOW_TAG = "AttachmentCleanupNow"
        private const val CLEANUP_INTERVAL_HOURS = 24L
        private const val INITIAL_BACKOFF_DELAY_MINUTES = 5L
    }
}
