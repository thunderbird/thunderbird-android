package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.fsck.k9.K9
import com.fsck.k9.K9.LockScreenNotificationVisibility

internal class LockScreenNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val resourceProvider: NotificationResourceProvider
) {
    fun configureLockScreenNotification(builder: NotificationCompat.Builder, notificationData: NotificationData) {
        when (K9.lockScreenNotificationVisibility) {
            LockScreenNotificationVisibility.NOTHING -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }
            LockScreenNotificationVisibility.APP_NAME -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            }
            LockScreenNotificationVisibility.EVERYTHING -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            LockScreenNotificationVisibility.SENDERS -> {
                val publicNotification = createPublicNotificationWithSenderList(notificationData)
                builder.setPublicVersion(publicNotification)
            }
            LockScreenNotificationVisibility.MESSAGE_COUNT -> {
                val publicNotification = createPublicNotificationWithNewMessagesCount(notificationData)
                builder.setPublicVersion(publicNotification)
            }
        }
    }

    private fun createPublicNotificationWithSenderList(notificationData: NotificationData): Notification {
        val builder = createPublicNotification(notificationData)

        val newMessages = notificationData.newMessagesCount
        if (newMessages == 1) {
            val holder = notificationData.holderForLatestNotification
            builder.setContentText(holder.content.sender)
        } else {
            val contents = notificationData.getContentForSummaryNotification()
            val senderList = createCommaSeparatedListOfSenders(contents)
            builder.setContentText(senderList)
        }

        return builder.build()
    }

    private fun createPublicNotificationWithNewMessagesCount(notificationData: NotificationData): Notification {
        val builder = createPublicNotification(notificationData)
        val account = notificationData.account
        val accountName = notificationHelper.getAccountName(account)

        builder.setContentText(accountName)
        return builder.build()
    }

    private fun createPublicNotification(notificationData: NotificationData): NotificationCompat.Builder {
        val account = notificationData.account
        val newMessagesCount = notificationData.newMessagesCount
        val title = resourceProvider.newMessagesTitle(newMessagesCount)

        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(account.chipColor)
            .setNumber(newMessagesCount)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
    }

    fun createCommaSeparatedListOfSenders(contents: List<NotificationContent>): String {
        return contents.asSequence()
            .map { it.sender }
            .distinct()
            .take(MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION)
            .joinToString()
    }

    companion object {
        const val MAX_NUMBER_OF_SENDERS_IN_LOCK_SCREEN_NOTIFICATION = 5
    }
}
