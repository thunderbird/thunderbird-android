package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat

internal class LockScreenNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val resourceProvider: NotificationResourceProvider,
) {
    fun configureLockScreenNotification(
        builder: NotificationCompat.Builder,
        baseNotificationData: BaseNotificationData,
    ) {
        when (baseNotificationData.lockScreenNotificationData) {
            LockScreenNotificationData.None -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_SECRET)
            }
            LockScreenNotificationData.AppName -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            }
            LockScreenNotificationData.Public -> {
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            is LockScreenNotificationData.SenderNames -> {
                val publicNotification = createPublicNotificationWithSenderList(baseNotificationData)
                builder.setPublicVersion(publicNotification)
            }
            LockScreenNotificationData.MessageCount -> {
                val publicNotification = createPublicNotificationWithNewMessagesCount(baseNotificationData)
                builder.setPublicVersion(publicNotification)
            }
        }
    }

    private fun createPublicNotificationWithSenderList(baseNotificationData: BaseNotificationData): Notification {
        val notificationData = baseNotificationData.lockScreenNotificationData as LockScreenNotificationData.SenderNames
        return createPublicNotification(baseNotificationData)
            .setContentText(notificationData.senderNames)
            .build()
    }

    private fun createPublicNotificationWithNewMessagesCount(baseNotificationData: BaseNotificationData): Notification {
        return createPublicNotification(baseNotificationData)
            .setContentText(baseNotificationData.accountName)
            .build()
    }

    private fun createPublicNotification(baseNotificationData: BaseNotificationData): NotificationCompat.Builder {
        val account = baseNotificationData.account
        val newMessagesCount = baseNotificationData.newMessagesCount
        val title = resourceProvider.newMessagesTitle(newMessagesCount)

        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setNumber(newMessagesCount)
            .setContentTitle(title)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
    }
}
