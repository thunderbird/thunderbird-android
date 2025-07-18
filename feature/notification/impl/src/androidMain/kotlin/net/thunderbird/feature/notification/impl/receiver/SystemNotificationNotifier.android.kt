package net.thunderbird.feature.notification.impl.receiver

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thunderbird.core.common.provider.ContextProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.NotificationStyle
import net.thunderbird.feature.notification.impl.intent.SystemNotificationIntentCreator
import net.thunderbird.feature.notification.impl.ui.action.NotificationActionCreator
import org.koin.core.component.KoinComponent
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject

private const val TAG = "SystemNotificationNotifier"
internal actual inline fun <reified TContext> SystemNotificationNotifier(
    logger: Logger,
    contextProvider: ContextProvider<TContext>,
): SystemNotificationNotifier {
    require(TContext::class == Context::class) {
        "SystemNotificationNotifier expects an Android Context."
    }
    return AndroidSystemNotificationNotifier(logger, contextProvider.context as Context)
}

internal class AndroidSystemNotificationNotifier(
    private val logger: Logger,
    private val context: Context,
    notificationIntentCreators: Lazy<List<SystemNotificationIntentCreator<SystemNotification>>> = inject(
        clazz = List::class.java,
        qualifier = named<SystemNotificationIntentCreator.TypeQualifier>(),
    ),
    notificationActionCreator: Lazy<NotificationActionCreator<SystemNotification>> = inject(
        clazz = NotificationActionCreator::class.java,
        qualifier = named(NotificationActionCreator.TypeQualifier.System),
    ),
) : SystemNotificationNotifier, KoinComponent {
    private val notificationIntentCreators by notificationIntentCreators
    private val notificationActionCreator by notificationActionCreator
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

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
        return NotificationCompat
            .Builder(context, channel.id)
            .apply {
                setSmallIcon(icon.resolve(notification = this@toAndroidNotification))
                setContentTitle(title)
                setTicker(accessibilityText)
                contentText?.let(::setContentText)
                subText?.let(::setSubText)
                setOngoing(severity.dismissable.not())
                setWhen(createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
                if (this@toAndroidNotification != lockscreenNotification) {
                    setPublicVersion(lockscreenNotification.toAndroidNotification())
                }

                notificationIntentCreators
                    .firstOrNull { it.accept(this@toAndroidNotification) }
                    ?.let { creator -> setContentIntent(creator.create(notification = this@toAndroidNotification)) }

                setNotificationStyle(notification = this@toAndroidNotification)

                if (actions.isNotEmpty()) {
                    for (action in actions) {
                        val notificationAction = notificationActionCreator
                            .create(notification = this@toAndroidNotification, action)

                        addAction(
                            /* icon = */
                            notificationAction.icon,
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
            is NotificationStyle.System.BigTextStyle -> setStyle(
                NotificationCompat.BigTextStyle().bigText(style.text),
            )

            is NotificationStyle.System.InboxStyle -> {
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(style.bigContentTitle)
                    .setSummaryText(style.summary)

                style.lines.forEach(inboxStyle::addLine)

                setStyle(inboxStyle)
            }

            NotificationStyle.System.Undefined -> Unit
        }
    }
}
