package com.fsck.k9.notification

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.WearableExtender
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.notification.NotificationChannelManager.ChannelType
import timber.log.Timber
import androidx.core.app.NotificationCompat.Builder as NotificationBuilder

internal class SingleMessageNotificationCreator(
    private val notificationHelper: NotificationHelper,
    private val actionCreator: NotificationActionCreator,
    private val resourceProvider: NotificationResourceProvider,
    private val lockScreenNotificationCreator: LockScreenNotificationCreator,
    private val notificationManager: NotificationManagerCompat
) {
    fun createSingleNotification(
        baseNotificationData: BaseNotificationData,
        singleNotificationData: SingleNotificationData,
        isGroupSummary: Boolean = false
    ) {
        val account = baseNotificationData.account
        val notificationId = singleNotificationData.notificationId
        val content = singleNotificationData.content

        val notification = notificationHelper.createNotificationBuilder(account, ChannelType.MESSAGES)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
            .setAutoCancel(true)
            .setGroup(baseNotificationData.groupKey)
            .setGroupSummary(isGroupSummary)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(baseNotificationData.color)
            .setWhen(singleNotificationData.timestamp)
            .setTicker(content.summary)
            .setContentTitle(content.sender)
            .setContentText(content.subject)
            .setSubText(baseNotificationData.accountName)
            .setBigText(content.preview)
            .setContentIntent(createViewIntent(content, notificationId))
            .setDeleteIntent(createDismissIntent(content, notificationId))
            .setDeviceActions(singleNotificationData)
            .setWearActions(singleNotificationData)
            .setAppearance(singleNotificationData.isSilent, baseNotificationData.appearance)
            .setLockScreenNotification(baseNotificationData, singleNotificationData.addLockScreenNotification)
            .build()

        if (isGroupSummary) {
            Timber.v(
                "Creating single summary notification (silent=%b): %s",
                singleNotificationData.isSilent,
                notification
            )
        }
        notificationManager.notify(notificationId, notification)
    }

    private fun NotificationBuilder.setBigText(text: CharSequence) = apply {
        setStyle(NotificationCompat.BigTextStyle().bigText(text))
    }

    private fun createViewIntent(content: NotificationContent, notificationId: Int): PendingIntent {
        return actionCreator.createViewMessagePendingIntent(content.messageReference, notificationId)
    }

    private fun createDismissIntent(content: NotificationContent, notificationId: Int): PendingIntent {
        return actionCreator.createDismissMessagePendingIntent(content.messageReference, notificationId)
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
        val replyToMessagePendingIntent =
            actionCreator.createReplyPendingIntent(messageReference, notificationData.notificationId)

        addAction(icon, title, replyToMessagePendingIntent)
    }

    private fun NotificationBuilder.addMarkAsReadAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val content = notificationData.content
        val notificationId = notificationData.notificationId
        val messageReference = content.messageReference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId)

        addAction(icon, title, action)
    }

    private fun NotificationBuilder.addDeleteAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val content = notificationData.content
        val notificationId = notificationData.notificationId
        val messageReference = content.messageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId)

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
        val notificationId = notificationData.notificationId
        val action = actionCreator.createReplyPendingIntent(messageReference, notificationId)
        val replyAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(replyAction)
    }

    private fun WearableExtender.addMarkAsReadAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReference = notificationData.content.messageReference
        val notificationId = notificationData.notificationId
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(markAsReadAction)
    }

    private fun WearableExtender.addDeleteAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDelete()
        val messageReference = notificationData.content.messageReference
        val notificationId = notificationData.notificationId
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(deleteAction)
    }

    private fun WearableExtender.addArchiveAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchive()
        val messageReference = notificationData.content.messageReference
        val notificationId = notificationData.notificationId
        val action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(archiveAction)
    }

    private fun WearableExtender.addMarkAsSpamAction(notificationData: SingleNotificationData) {
        val icon = resourceProvider.wearIconMarkAsSpam
        val title = resourceProvider.actionMarkAsSpam()
        val messageReference = notificationData.content.messageReference
        val notificationId = notificationData.notificationId
        val action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId)
        val spamAction = NotificationCompat.Action.Builder(icon, title, action).build()

        addAction(spamAction)
    }

    private fun NotificationBuilder.setLockScreenNotification(
        notificationData: BaseNotificationData,
        addLockScreenNotification: Boolean
    ) = apply {
        if (addLockScreenNotification) {
            lockScreenNotificationCreator.configureLockScreenNotification(this, notificationData)
        }
    }
}
