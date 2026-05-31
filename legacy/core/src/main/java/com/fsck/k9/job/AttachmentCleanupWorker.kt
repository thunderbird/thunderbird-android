package com.fsck.k9.job

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteDatabaseLockedException
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import java.util.concurrent.TimeUnit
import net.thunderbird.core.android.account.AccountDefaultsProvider.Companion.MIN_ATTACHMENT_CLEANUP_DAYS
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.preference.BackgroundOps
import net.thunderbird.core.preference.GeneralSettingsManager

// IMPORTANT: Update K9WorkerFactory when moving this class and the FQCN no longer starts with "com.fsck.k9".
class AttachmentCleanupWorker(
    private val preferences: Preferences,
    private val messageStoreManager: MessageStoreManager,
    private val generalSettingsManager: GeneralSettingsManager,
    context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {

    override fun doWork(): Result {
        if (isBackgroundCleanupDisabled()) {
            return Result.success()
        }

        val accountUuid = inputData.getString(EXTRA_ACCOUNT_UUID) ?: return Result.failure()
        val account = preferences.getAccount(accountUuid) ?: return Result.success()
        val retentionDays = account.attachmentCleanupDays

        if (retentionDays < MIN_ATTACHMENT_CLEANUP_DAYS) {
            return Result.success()
        }

        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            val result = messageStoreManager.getMessageStore(
                account,
            ).removeOldDownloadedAttachments(cutoffTime, MAX_PARTS_PER_RUN)

            if (result.changedPartCount > 0) {
                Log.d("Cleaned %d locally cached attachment parts for account %s", result.changedPartCount, accountUuid)
            }

            Result.success()
        } catch (e: SQLiteDatabaseLockedException) {
            retryTransientFailureOrFail(e)
        } catch (e: Exception) {
            Log.e(e, "Failed to clean up old downloaded attachments")
            Result.failure()
        }
    }

    private fun retryTransientFailureOrFail(exception: Exception): Result {
        return if (runAttemptCount < MAX_FAILURE_RETRY_ATTEMPTS) {
            Log.w(exception, "Attachment cleanup database is locked")
            Result.retry()
        } else {
            Log.e(exception, "Attachment cleanup database remained locked after retries")
            Result.failure()
        }
    }

    private fun isBackgroundCleanupDisabled(): Boolean {
        return when (generalSettingsManager.getConfig().network.backgroundOps) {
            BackgroundOps.NEVER -> true
            BackgroundOps.ALWAYS -> false
            BackgroundOps.WHEN_CHECKED_AUTO_SYNC -> !ContentResolver.getMasterSyncAutomatically()
        }
    }

    companion object {
        const val EXTRA_ACCOUNT_UUID = "accountUuid"
        private const val MAX_PARTS_PER_RUN = 3_000
        private const val MAX_FAILURE_RETRY_ATTEMPTS = 3
    }
}
