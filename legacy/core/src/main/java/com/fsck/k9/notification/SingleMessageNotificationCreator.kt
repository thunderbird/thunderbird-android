package com.fsck.k9.notification

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.WearableExtender
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import net.thunderbird.core.logging.legacy.Log
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal class SingleMessageNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator,
) {
    fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData,
        isGroupSummary: Boolean = false,
    ) {
        val account = baseNotificationData.account
        val notificationId = singleNotificationData.notificationId
        val content = singleNotificationData.content

        val notification = notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(isGroupSummary)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setWhen(singleNotificationData.timestamp)
            .setTicker(content.summary)
            .setContentTitle(content.sender)
            .setContentText(content.subject)
            .setSubText(baseNotificationData.accountName)
            .setBigText(content.preview)
            .setContentIntent(actionCreator.createViewMessagePendingIntent(content.messageReference))
            .setDeleteIntent(actionCreator.createDismissMessagePendingIntent(content.messageReference))
            .setDeviceActions(singleNotificationData)
            .setWearActions(singleNotificationData)
            .setAppearance(singleNotificationData.isSilent, baseNotificationData.appearance)
            .setLockScreenNotification(baseNotificationData, singleNotificationData.addLockScreenNotification)
            .build()

        if (isGroupSummary) {
            Log.v(
                "Creating single summary notification (silent=%b): %s",
                singleNotificationData.isSilent,
                notification,
            )
        }
        notificationHelper.notify(account, notificationId, notification)
    }

    private fun NotificationBuilder.setBigText(text: CharSequence) = apply {
        setStyle(NotificationCompat.BigTextStyle().bigText(text))
    }

    private fun NotificationBuilder.setDeviceActions(notificationData: SingleNotificationData) = apply {
        val actions = notificationData.actions
        for (action in actions) {
            when (action) {
                NotificationAction.Reply -> addReplyAction(notificationData)
                NotificationAction.MarkAsRead -> addMarkAsReadAction(notificationData)
                NotificationAction.Delete -> addDeleteAction(notificationData)
            }
        }
    }

    private fun NotificationBuilder.addReplyAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.iconReply
        val title = resourceProvider.actionReply()
        val content = notificationData.content
        val messageReference = content.messageReference
        val replyToMessagePendingIntent = actionCreator.createReplyPendingIntent(messageReference)

        addAction(icon, title, replyToMessagePendingIntent)
    }

    private fun NotificationBuilder.addMarkAsReadAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val content = notificationData.content
        val messageReference = content.messageReference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference)

        addAction(icon, title, action)
    }

    private fun NotificationBuilder.addDeleteAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val content = notificationData.content
        val messageReference = content.messageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference)

        addAction(icon, title, action)
    }

    private fun NotificationBuilder.setWearActions(notificationData: SingleNotificationData) = apply {
        val wearableExtender = WearableExtender().apply {
            for (action in notificationData.wearActions) {
                when (action) {
                    WearNotificationAction.Reply -> addReplyAction(notificationData)
                    WearNotificationAction.MarkAsRead -> addMarkAsReadAction(notificationData)
                    WearNotificationAction.Delete -> addDeleteAction(notificationData)
                    WearNotificationAction.Archive -> addArchiveAction(notificationData)
                    WearNotificationAction.Spam -> addMarkAsSpamAction(notificationData)
                }
            }
        }

        extend(wearableExtender)
    }

    private fun WearableExtender.addReplyAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconReplyAll
        val title = resourceProvider.actionReply()
        val messageReference = notificationData.content.messageReference
        val action = actionCreator.createReplyPendingIntent(messageReference)
        val replyAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(replyAction)
    }

    private fun WearableExtender.addMarkAsReadAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReference = notificationData.content.messageReference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun WearableExtender.addDeleteAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDelete()
        val messageReference = notificationData.content.messageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun WearableExtender.addArchiveAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchive()
        val messageReference = notificationData.content.messageReference
        val action = actionCreator.createArchiveMessagePendingIntent(messageReference)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }

    private fun WearableExtender.addMarkAsSpamAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconMarkAsSpam
        val title = resourceProvider.actionMarkAsSpam()
        val messageReference = notificationData.content.messageReference
        val action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference)
        val spamAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(spamAction)
    }

    private fun NotificationBuilder.setLockScreenNotification(
        notificationData: BaseNotificationData,
        addLockScreenNotification: Boolean,
    ) = apply {
        if (addLockScreenNotification) {
            lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
        }
    }
}
