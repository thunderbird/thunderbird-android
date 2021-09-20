package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.K9

internal open class SingleMessageNotifications(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    resourceProvider: NotificationResourceProvider
) : BaseNotifications(notificationHelper, actionCreator, resourceProvider) {

    fun buildSingleMessageNotification(account: Account, holder: NotificationHolder): Notification {
        val notificationId = holder.notificationId
        return createSingleMessageNotificationBuilder(account, holder, notificationId)
            .setNotificationSilent()
            .build()
    }

    fun createSingleMessageNotificationBuilder(
        account: Account,
        holder: NotificationHolder,
        notificationId: Int
    ): NotificationCompat.Builder {
        val content = holder.content
        val builder = createBigTextStyleNotification(account, holder, notificationId)

        val deletePendingIntent = actionCreator.createDismissMessagePendingIntent(
            context, content.messageReference, holder.notificationId
        )
        builder.setDeleteIntent(deletePendingIntent)

        addActions(builder, account, holder)

        return builder
    }

    private fun addActions(builder: NotificationCompat.Builder, account: Account, holder: NotificationHolder) {
        addDeviceActions(builder, holder)
        addWearActions(builder, account, holder)
    }

    private fun addDeviceActions(builder: NotificationCompat.Builder, holder: NotificationHolder) {
        addDeviceReplyAction(builder, holder)
        addDeviceMarkAsReadAction(builder, holder)
        addDeviceDeleteAction(builder, holder)
    }

    private fun addDeviceReplyAction(builder: NotificationCompat.Builder, holder: NotificationHolder) {
        val icon = resourceProvider.iconReply
        val title = resourceProvider.actionReply()
        val content = holder.content
        val messageReference = content.messageReference
        val replyToMessagePendingIntent =
            actionCreator.createReplyPendingIntent(messageReference, holder.notificationId)

        builder.addAction(icon, title, replyToMessagePendingIntent)
    }

    private fun addDeviceMarkAsReadAction(builder: NotificationCompat.Builder, holder: NotificationHolder) {
        val icon = resourceProvider.iconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val content = holder.content
        val notificationId = holder.notificationId
        val messageReference = content.messageReference
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId)

        builder.addAction(icon, title, action)
    }

    private fun addDeviceDeleteAction(builder: NotificationCompat.Builder, holder: NotificationHolder) {
        if (!isDeleteActionEnabled()) {
            return
        }

        val icon = resourceProvider.iconDelete
        val title = resourceProvider.actionDelete()
        val content = holder.content
        val notificationId = holder.notificationId
        val messageReference = content.messageReference
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId)

        builder.addAction(icon, title, action)
    }

    private fun addWearActions(builder: NotificationCompat.Builder, account: Account, holder: NotificationHolder) {
        val wearableExtender = NotificationCompat.WearableExtender()

        addReplyAction(wearableExtender, holder)
        addMarkAsReadAction(wearableExtender, holder)

        if (isDeleteActionAvailableForWear()) {
            addDeleteAction(wearableExtender, holder)
        }

        if (isArchiveActionAvailableForWear(account)) {
            addArchiveAction(wearableExtender, holder)
        }

        if (isSpamActionAvailableForWear(account)) {
            addMarkAsSpamAction(wearableExtender, holder)
        }

        builder.extend(wearableExtender)
    }

    private fun addReplyAction(wearableExtender: NotificationCompat.WearableExtender, holder: NotificationHolder) {
        val icon = resourceProvider.wearIconReplyAll
        val title = resourceProvider.actionReply()
        val messageReference = holder.content.messageReference
        val notificationId = holder.notificationId
        val action = actionCreator.createReplyPendingIntent(messageReference, notificationId)
        val replyAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(replyAction)
    }

    private fun addMarkAsReadAction(wearableExtender: NotificationCompat.WearableExtender, holder: NotificationHolder) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAsRead()
        val messageReference = holder.content.messageReference
        val notificationId = holder.notificationId
        val action = actionCreator.createMarkMessageAsReadPendingIntent(messageReference, notificationId)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(markAsReadAction)
    }

    private fun addDeleteAction(wearableExtender: NotificationCompat.WearableExtender, holder: NotificationHolder) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDelete()
        val messageReference = holder.content.messageReference
        val notificationId = holder.notificationId
        val action = actionCreator.createDeleteMessagePendingIntent(messageReference, notificationId)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(deleteAction)
    }

    private fun addArchiveAction(wearableExtender: NotificationCompat.WearableExtender, holder: NotificationHolder) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchive()
        val messageReference = holder.content.messageReference
        val notificationId = holder.notificationId
        val action = actionCreator.createArchiveMessagePendingIntent(messageReference, notificationId)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(archiveAction)
    }

    private fun addMarkAsSpamAction(wearableExtender: NotificationCompat.WearableExtender, holder: NotificationHolder) {
        val icon = resourceProvider.wearIconMarkAsSpam
        val title = resourceProvider.actionMarkAsSpam()
        val messageReference = holder.content.messageReference
        val notificationId = holder.notificationId
        val action = actionCreator.createMarkMessageAsSpamPendingIntent(messageReference, notificationId)
        val spamAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(spamAction)
    }

    private fun isDeleteActionAvailableForWear(): Boolean {
        return isDeleteActionEnabled() && !K9.isConfirmDeleteFromNotification
    }

    private fun isArchiveActionAvailableForWear(account: Account): Boolean {
        return account.archiveFolderId != null
    }

    private fun isSpamActionAvailableForWear(account: Account): Boolean {
        return account.spamFolderId != null && !K9.isConfirmSpam
    }
}
