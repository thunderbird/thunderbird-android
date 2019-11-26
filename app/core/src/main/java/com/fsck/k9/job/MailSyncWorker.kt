package com.fsck.k9.job

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.service.CoreService
import timber.log.Timber

class MailSyncWorker(
    private val messagingController: MessagingController,
    private val preferences: Preferences,
    context: Context,
    parameters: WorkerParameters
) : Worker(context, parameters) {

    override fun doWork(): Result {
        val accountUuid = inputData.getString(EXTRA_ACCOUNT_UUID)
        requireNotNull(accountUuid)

        Timber.d("Executing periodic mail sync for account %s", accountUuid)

        if (!CoreService.isBackgroundSyncAllowed()) {
            Timber.d("Background sync is disabled. Skipping mail sync.")
            return Result.success()
        }

        preferences.getAccount(accountUuid)?.let { account ->
            messagingController.checkMailBlocking(account)
        }

        return Result.success()
    }

    companion object {
        const val EXTRA_ACCOUNT_UUID = "accountUuid"
    }
}
