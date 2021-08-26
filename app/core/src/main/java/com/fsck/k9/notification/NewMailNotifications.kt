package com.fsck.k9.notification

import android.util.SparseArray
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mailstore.LocalMessage

/**
 * Handle notifications for new messages.
 *
 * We call the notification shown on the device *summary notification*, even when there's only one new message.
 * Notifications on an Android Wear device are displayed as a stack of cards and that's why we call them *stacked
 * notifications*. We have to keep track of stacked notifications individually and recreate/update the summary
 * notification when one or more of the stacked notifications are added/removed.
 *
 * [NotificationData] keeps track of all data required to (re)create the actual system notifications.
 */
internal open class NewMailNotifications(
    private val notificationHelper: NotificationHelper,
    private val contentCreator: NotificationContentCreator,
    private val deviceNotifications: DeviceNotifications,
    private val wearNotifications: WearNotifications
) {
    private val notifications = SparseArray<NotificationData>()
    private val lock = Any()

    fun addNewMailNotification(account: Account, message: LocalMessage, unreadMessageCount: Int) {
        val content = contentCreator.createFromMessage(account, message)

        synchronized(lock) {
            val notificationData = getOrCreateNotificationData(account, unreadMessageCount)

            val result = notificationData.addNotificationContent(content)
            if (result.shouldCancelNotification) {
                val notificationId = result.notificationId
                cancelNotification(notificationId)
            }

            createStackedNotification(account, result.notificationHolder)
            createSummaryNotification(account, notificationData, false)
        }
    }

    fun removeNewMailNotification(account: Account, messageReference: MessageReference) {
        synchronized(lock) {
            val notificationData = getNotificationData(account) ?: return

            val result = notificationData.removeNotificationForMessage(messageReference)
            if (result.isUnknownNotification) return

            cancelNotification(result.notificationId)

            if (result.shouldCreateNotification) {
                createStackedNotification(account, result.notificationHolder)
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

    private fun getOrCreateNotificationData(account: Account, unreadMessageCount: Int): NotificationData {
        val notificationData = getNotificationData(account)
        if (notificationData != null) return notificationData

        val accountNumber = account.accountNumber
        val newNotificationHolder = createNotificationData(account, unreadMessageCount)
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

    protected open fun createNotificationData(account: Account, unreadMessageCount: Int): NotificationData {
        return NotificationData(account, unreadMessageCount)
    }

    private fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    private fun updateSummaryNotification(account: Account, notificationData: NotificationData) {
        if (notificationData.newMessagesCount == 0) {
            clearNewMailNotifications(account)
        } else {
            createSummaryNotification(account, notificationData, true)
        }
    }

    private fun createSummaryNotification(account: Account, notificationData: NotificationData, silent: Boolean) {
        val notification = deviceNotifications.buildSummaryNotification(account, notificationData, silent)
        val notificationId = NotificationIds.getNewMailSummaryNotificationId(account)
        notificationManager.notify(notificationId, notification)
    }

    private fun createStackedNotification(account: Account, holder: NotificationHolder) {
        if (isPrivacyModeEnabled) {
            return
        }

        val notification = wearNotifications.buildStackedNotification(account, holder)
        val notificationId = holder.notificationId
        notificationManager.notify(notificationId, notification)
    }

    private val isPrivacyModeEnabled: Boolean
        get() = K9.notificationHideSubject !== K9.NotificationHideSubject.NEVER

    private val notificationManager: NotificationManagerCompat
        get() = notificationHelper.getNotificationManager()
}
