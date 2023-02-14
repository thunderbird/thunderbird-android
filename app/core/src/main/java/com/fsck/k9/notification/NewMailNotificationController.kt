package com.fsck.k9.notification

import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage

/**
 * Handle notifications for new messages.
 */
internal class NewMailNotificationController(
    private val notificationManager: NotificationManagerCompat,
    private val newMailNotificationManager: NewMailNotificationManager,
    private val summaryNotificationCreator: SummaryNotificationCreator,
    private val singleMessageNotificationCreator: SingleMessageNotificationCreator,
) {
    @Synchronized
    fun restoreNewMailNotifications(accounts: List<Account>) {
        for (account in accounts) {
            val notificationData = newMailNotificationManager.restoreNewMailNotifications(account)

            if (notificationData != null) {
                processNewMailNotificationData(notificationData)
            }
        }
    }

    @Synchronized
    fun addNewMailNotification(account: Account, message: LocalMessage, silent: Boolean) {
        val notificationData = newMailNotificationManager.addNewMailNotification(account, message, silent)

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun removeNewMailNotifications(
        account: Account,
        clearNewMessageState: Boolean,
        selector: (List<MessageReference>) -> List<MessageReference>,
    ) {
        val notificationData = newMailNotificationManager.removeNewMailNotifications(
            account,
            clearNewMessageState,
            selector,
        )

        if (notificationData != null) {
            processNewMailNotificationData(notificationData)
        }
    }

    @Synchronized
    fun clearNewMailNotifications(account: Account, clearNewMessageState: Boolean) {
        val cancelNotificationIds = newMailNotificationManager.clearNewMailNotifications(account, clearNewMessageState)

        cancelNotifications(cancelNotificationIds)
    }

    private fun processNewMailNotificationData(notificationData: NewMailNotificationData) {
        cancelNotifications(notificationData.cancelNotificationIds)

        for (singleNotificationData in notificationData.singleNotificationData) {
            createSingleNotification(notificationData.baseNotificationData, singleNotificationData)
        }

        notificationData.summaryNotificationData?.let { summaryNotificationData ->
            createSummaryNotification(notificationData.baseNotificationData, summaryNotificationData)
        }
    }

    private fun cancelNotifications(notificationIds: List<Int>) {
        for (notificationId in notificationIds) {
            notificationManager.cancel(notificationId)
        }
    }

    private fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData,
    ) {
        singleMessageNotificationCreator.createSingleNotification(baseNotificationData, singleNotificationData)
    }

    private fun createSummaryNotification(
        baseNotificationData: BaseNotificationData,
        summaryNotificationData: SummaryNotificationData,
    ) {
        summaryNotificationCreator.createSummaryNotification(baseNotificationData, summaryNotificationData)
    }
}
