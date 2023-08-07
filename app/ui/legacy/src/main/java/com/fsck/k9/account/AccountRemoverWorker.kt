package com.fsck.k9.account

import android.content.Context
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fsck.k9.notification.BackgroundWorkNotificationController
import com.fsck.k9.ui.R

/**
 * A [Worker] to remove an account in the background.
 */
class AccountRemoverWorker(
    private val accountRemover: AccountRemover,
    private val notificationController: BackgroundWorkNotificationController,
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val accountUuid = requireNotNull(inputData.getString(ARG_ACCOUNT_UUID)) { "No account UUID provided" }

        accountRemover.removeAccount(accountUuid)

        return Result.success()
    }

    override fun getForegroundInfo(): ForegroundInfo {
        val notificationText = applicationContext.getString(R.string.background_work_notification_remove_account)
        val notificationId = notificationController.notificationId
        val notification = notificationController.createNotification(notificationText)
        return ForegroundInfo(notificationId, notification)
    }

    companion object {
        private const val ARG_ACCOUNT_UUID = "accountUuid"

        fun enqueueRemoveAccountWorker(context: Context, accountUuid: String) {
            val data = Data.Builder()
                .putString(ARG_ACCOUNT_UUID, accountUuid)
                .build()

            val request = OneTimeWorkRequest.Builder(AccountRemoverWorker::class.java)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
