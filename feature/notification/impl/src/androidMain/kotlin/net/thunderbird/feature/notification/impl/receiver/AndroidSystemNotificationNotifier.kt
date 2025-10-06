package net.thunderbird.feature.notification.impl.receiver

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator

private const val TAG = "AndroidSystemNotificationNotifier"

@OptIn(ExperimentalTime::class)
internal class AndroidSystemNotificationNotifier(
    private val logger: Logger,
    private val applicationContext: Context,
    private val notificationActionCreator: NotificationActionCreator<SystemNotification>,
) : SystemNotificationNotifier {
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)

    override suspend fun show(
        id: NotificationId,
        notification: SystemNotification,
    ) {
        logger.debug(TAG) { "show() called with: id = $id, notification = $notification" }
        val androidNotification = notification.toAndroidNotification()
        notificationManager.notify(id.value, androidNotification)
    }

    override fun dispose() {
        logger.debug(TAG) { "dispose() called" }
    }

    private suspend fun SystemNotification.toAndroidNotification(): Notification {
        logger.debug(TAG) { "toAndroidNotification() called with systemNotification = $this" }
        val systemNotification = this
        return NotificationCompat
            .Builder(applicationContext, channel.id)
            .apply {
                setSmallIcon(
                    checkNotNull(icon.systemNotificationIcon) {
                        "A icon is required to display a system notification"
                    },
                )
                setContentTitle(title)
                setTicker(accessibilityText)
                contentText?.let(::setContentText)
                subText?.let(::setSubText)
                setOngoing(severity.dismissable.not())
                setWhen(createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
                asLockscreenNotification()?.let { lockscreenNotification ->
                    if (lockscreenNotification.notification != systemNotification) {
                        setPublicVersion(lockscreenNotification.notification.toAndroidNotification())
                    }
                }

                val tapAction = notificationActionCreator.create(
                    notification = systemNotification,
                    action = NotificationAction.Tap,
                )
                setContentIntent(tapAction.pendingIntent)

                setNotificationStyle(notification = systemNotification)

                if (actions.isNotEmpty()) {
                    for (action in actions) {
                        val notificationAction = notificationActionCreator
                            .create(notification = systemNotification, action)

                        addAction(
                            /* icon = */
                            notificationAction.icon ?: 0,
                            /* title = */
                            notificationAction.title,
                            /* intent = */
                            notificationAction.pendingIntent,
                        )
                    }
                }
            }
            .build()
    }

    private fun NotificationCompat.Builder.setNotificationStyle(
        notification: SystemNotification,
    ) {
        when (val style = notification.systemNotificationStyle) {
            is SystemNotificationStyle.BigTextStyle -> setStyle(
                NotificationCompat.BigTextStyle().bigText(style.text),
            )

            is SystemNotificationStyle.InboxStyle -> {
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(style.bigContentTitle)
                    .setSummaryText(style.summary)

                style.lines.forEach(inboxStyle::addLine)

                setStyle(inboxStyle)
            }

            SystemNotificationStyle.Undefined -> Unit
        }
    }
}
