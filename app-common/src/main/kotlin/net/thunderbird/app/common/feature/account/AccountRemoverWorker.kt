package net.thunderbird.app.common.feature.account

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
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

/**
 * A [androidx.work.Worker] to remove an account in the background.
 */
// IMPORTANT: Update K9WorkerFactory when moving this class and the FQCN no longer starts with "com.fsck.k9".
class AccountRemoverWorker(
    private val accountRemover: AccountRemover,
    private val notificationController: BackgroundWorkNotificationController,
    context: Context,
    workerParams: WorkerParameters,
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val accountId = requireNotNull(
            inputData.getString(ARG_ACCOUNT_UUID)?.let { AccountIdFactory.of(it) },
        ) { "No account UUID provided" }

        accountRemover.removeAccount(accountId)

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

        fun enqueueRemoveAccountWorker(context: Context, accountId: AccountId) {
            val data = Data.Builder()
                .putString(ARG_ACCOUNT_UUID, accountId.toString())
                .build()

            val request = OneTimeWorkRequest.Builder(AccountRemoverWorker::class.java)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
