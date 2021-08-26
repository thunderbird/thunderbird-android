package com.fsck.k9.notification

import android.content.Context
import androidx.core.app.NotificationCompat
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.K9.NotificationQuickDelete

internal abstract class BaseNotifications(
    protected val notificationHelper: NotificationHelper,
    protected val actionCreator: NotificationActionCreator,
    protected val resourceProvider: NotificationResourceProvider
) {
    protected val context: Context = notificationHelper.getContext()

    fun createBigTextStyleNotification(
        account: Account,
        holder: NotificationHolder,
        notificationId: Int
    ): NotificationCompat.Builder {
        val accountName = notificationHelper.getAccountName(account)
        val content = holder.content
        val groupKey = NotificationGroupKeys.getGroupKey(account)

        val builder = createAndInitializeNotificationBuilder(account)
            .setTicker(content.summary)
            .setGroup(groupKey)
            .setContentTitle(content.sender)
            .setContentText(content.subject)
            .setSubText(accountName)

        val style = createBigTextStyle(builder)
        style.bigText(content.preview)
        builder.setStyle(style)

        val contentIntent = actionCreator.createViewMessagePendingIntent(content.messageReference, notificationId)
        builder.setContentIntent(contentIntent)

        return builder
    }

    fun createAndInitializeNotificationBuilder(account: Account): NotificationCompat.Builder {
        return notificationHelper.createNotificationBuilder(account, NotificationChannelManager.ChannelType.MESSAGES)
            .setSmallIcon(resourceProvider.iconNewMail)
            .setColor(account.chipColor)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_EMAIL)
    }

    fun isDeleteActionEnabled(): Boolean {
        val deleteOption = K9.notificationQuickDeleteBehaviour
        return deleteOption === NotificationQuickDelete.ALWAYS ||
            deleteOption === NotificationQuickDelete.FOR_SINGLE_MSG
    }

    protected open fun createBigTextStyle(builder: NotificationCompat.Builder?): NotificationCompat.BigTextStyle {
        return NotificationCompat.BigTextStyle(builder)
    }
}
