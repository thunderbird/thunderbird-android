package com.fsck.k9.job

import android.content.ContentResolver
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Preferences
import java.util.concurrent.TimeUnit
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
            Log.d("Background cleanup is disabled. Skipping attachment cleanup.")
            return Result.success()
        }

        val accountUuid = inputData.getString(EXTRA_ACCOUNT_UUID) ?: return Result.failure()
        val account = preferences.getAccount(accountUuid) ?: return Result.success()
        val retentionDays = account.attachmentCleanupDays

        if (retentionDays <= 0) {
            return Result.success()
        }

        return try {
            val cutoffTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
            val removedPartCount = messageStoreManager.getMessageStore(
                account,
            ).removeOldDownloadedAttachments(cutoffTime)
            Log.d("Removed %d locally cached attachment parts for account %s", removedPartCount, accountUuid)
            Result.success()
        } catch (e: Exception) {
            Log.e(e, "Failed to clean up old downloaded attachments")
            Result.retry()
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
    }
}
