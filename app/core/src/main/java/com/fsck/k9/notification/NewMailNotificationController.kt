package com.fsck.k9.notification

import android.util.SparseArray
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage

/**
 * Handle notifications for new messages.
 */
internal open class NewMailNotificationController(
    private val notificationHelper: NotificationHelper,
    private val contentCreator: NotificationContentCreator,
    private val summaryNotificationCreator: SummaryNotificationCreator,
    private val singleMessageNotificationCreator: SingleMessageNotificationCreator
) {
    private val notifications = SparseArray<NotificationData>()
    private val lock = Any()

    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        val content = contentCreator.createFromMessage(account, message)

        synchronized(lock) {
            val notificationData = getOrCreateNotificationData(account)

            val result = notificationData.addNotificationContent(content)
            if (result.shouldCancelNotification) {
                val notificationId = result.notificationId
                cancelNotification(notificationId)
            }

            if (notificationData.isSingleMessageNotification) {
                createSingleMessageNotificationWithLockScreenNotification(account, notificationData)
            } else {
                createSingleMessageNotification(account, result.notificationHolder)
            }

            createSummaryNotification(account, notificationData, silent)
        }
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        synchronized(lock) {
            val notificationData = getNotificationData(account) ?: return

            val result = notificationData.removeNotificationForMessage(messageReference)
            if (result.isUnknownNotification) return

            cancelNotification(result.notificationId)

            if (notificationData.isSingleMessageNotification) {
                createSingleMessageNotificationWithLockScreenNotification(account, notificationData)
            } else if (result.shouldCreateNotification) {
                createSingleMessageNotification(account, result.notificationHolder)
            }

            updateSummaryNotification(account, notificationData)
        }
    }

    fun clearNewMailNotifications(account: Account) {
        val notificationData = synchronized(lock) { removeNotificationData(account) } ?: return

        for (notificationId in notificationData.getActiveNotificationIds()) {
            cancelNotification(notificationId)
        }

        val notificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        cancelNotification(notificationId)
    }

    private fun getOrCreateNotificationData(account: Account): NotificationData {
        val notificationData = getNotificationData(account)
        if (notificationData != null) return notificationData

        val accountNumber = account.accountNumber
        val newNotificationHolder = createNotificationData(account)
        notifications.put(accountNumber, newNotificationHolder)

        return newNotificationHolder
    }

    private fun getNotificationData(account: Account): NotificationData? {
        val accountNumber = account.accountNumber
        return notifications[accountNumber]
    }

    private fun removeNotificationData(account: Account): NotificationData? {
        val accountNumber = account.accountNumber
        val notificationData = notifications[accountNumber]
        notifications.remove(accountNumber)
        return notificationData
    }

    protected open fun createNotificationData(account: Account): NotificationData {
        return NotificationData(account)
    }

    private fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun updateSummaryNotification(account: Account, notificationData: NotificationData) {
        if (notificationData.newMessagesCount == 0) {
            clearNewMailNotifications(account)
        } else {
            createSummaryNotification(account, notificationData, silent = true)
        }
    }

    private fun createSummaryNotification(account: Account, notificationData: NotificationData, silent: Boolean) {
        val notification = summaryNotificationCreator.buildSummaryNotification(account, notificationData, silent)
        val notificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        notificationManager.notify(notificationId, notification)
    }

    private fun createSingleMessageNotification(account: Account, holder: NotificationHolder) {
        val notification = singleMessageNotificationCreator.buildSingleMessageNotification(account, holder)
        val notificationId = holder.notificationId
        notificationManager.notify(notificationId, notification)
    }

    // When there's only one notification the "public version" of the notification that might be displayed on a secure
    // lockscreen isn't taken from the summary notification, but from the single "grouped" notification.
    private fun createSingleMessageNotificationWithLockScreenNotification(
        account: Account,
        notificationData: NotificationData
    ) {
        val holder = notificationData.holderForLatestNotification
        val notification = singleMessageNotificationCreator.buildSingleMessageNotificationWithLockScreenNotification(
            account,
            holder,
            notificationData
        )

        val notificationId = holder.notificationId
        notificationManager.notify(notificationId, notification)
    }

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
