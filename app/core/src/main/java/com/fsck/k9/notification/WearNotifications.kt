package com.fsck.k9.notification

import android.app.Notification
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.notification.NotificationIds.getNewMailSummaryNotificationId

internal open class WearNotifications(
    notificationHelper: NotificationHelper,
    actionCreator: NotificationActionCreator,
    resourceProvider: NotificationResourceProvider
) : BaseNotifications(notificationHelper, actionCreator, resourceProvider) {

    fun buildStackedNotification(account: Account, holder: NotificationHolder): Notification {
        val notificationId = holder.notificationId
        val content = holder.content
        val builder = createBigTextStyleNotification(account, holder, notificationId)
        builder.setNotificationSilent()

        val deletePendingIntent = actionCreator.createDismissMessagePendingIntent(
            context, content.messageReference, holder.notificationId
        )
        builder.setDeleteIntent(deletePendingIntent)

        addActions(builder, account, holder)

        return builder.build()
    }

    fun addSummaryActions(builder: NotificationCompat.Builder, notificationData: NotificationData) {
        val wearableExtender = NotificationCompat.WearableExtender()

        addMarkAllAsReadAction(wearableExtender, notificationData)

        if (isDeleteActionAvailableForWear()) {
            addDeleteAllAction(wearableExtender, notificationData)
        }

        if (isArchiveActionAvailableForWear(notificationData.account)) {
            addArchiveAllAction(wearableExtender, notificationData)
        }

        builder.extend(wearableExtender)
    }

    private fun addMarkAllAsReadAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconMarkAsRead
        val title = resourceProvider.actionMarkAllAsRead()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createMarkAllAsReadPendingIntent(account, messageReferences, notificationId)
        val markAsReadAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(markAsReadAction)
    }

    private fun addDeleteAllAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconDelete
        val title = resourceProvider.actionDeleteAll()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createDeleteAllPendingIntent(account, messageReferences, notificationId)
        val deleteAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(deleteAction)
    }

    private fun addArchiveAllAction(
        wearableExtender: NotificationCompat.WearableExtender,
        notificationData: NotificationData
    ) {
        val icon = resourceProvider.wearIconArchive
        val title = resourceProvider.actionArchiveAll()
        val account = notificationData.account
        val messageReferences = notificationData.getAllMessageReferences()
        val notificationId = getNewMailSummaryNotificationId(account)
        val action = actionCreator.createArchiveAllPendingIntent(account, messageReferences, notificationId)
        val archiveAction = NotificationCompat.Action.Builder(icon, title, action).build()

        wearableExtender.addAction(archiveAction)
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
        return isMovePossible(account, account.archiveFolderId)
    }

    private fun isSpamActionAvailableForWear(account: Account): Boolean {
        return !K9.isConfirmSpam && isMovePossible(account, account.spamFolderId)
    }

    private fun isMovePossible(account: Account, destinationFolderId: Long?): Boolean {
        if (destinationFolderId == null) {
            return false
        }

        val controller = createMessagingController()
        return controller.isMoveCapable(account)
    }

    protected open fun createMessagingController(): MessagingController {
        return MessagingController.getInstance(context)
    }
}
