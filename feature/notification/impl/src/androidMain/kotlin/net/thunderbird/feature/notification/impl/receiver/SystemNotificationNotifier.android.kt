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
import net.thunderbird.feature.notification.impl.intent.SystemNotificationIntentCreator
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
        List::class.java,
        named<SystemNotificationIntentCreator.TypeQualifier>(),
    ),
) : SystemNotificationNotifier, KoinComponent {
    private val notificationIntentCreators by notificationIntentCreators
    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)

    override fun show(
        id: NotificationId,
        notification: SystemNotification,
    ) {
        logger.debug(TAG) { "show() called with: id = $id, notification = $notification" }
        notificationManager.notify(id.value, notification.toAndroidNotification())
    }

    override fun dispose() {
        logger.debug(TAG) { "dispose() called" }
    }

    private fun SystemNotification.toAndroidNotification(): Notification {
        logger.debug(TAG) { "toAndroidNotification() called with systemNotification = $this" }
        return NotificationCompat
            .Builder(context, channel.id)
            .apply {
//                setSmallIcon(notification.icon) // TODO.
                setContentTitle(title)
                setTicker(accessibilityText)
                contentText?.let(::setContentText)
                setOngoing(severity.dismissable.not())
                setWhen(createdAt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds())
                if (this@toAndroidNotification != lockscreenNotification) {
                    setPublicVersion(lockscreenNotification.toAndroidNotification())
                }

                notificationIntentCreators
                    .firstOrNull { it.accept(this@toAndroidNotification) }
                    ?.let { creator -> setContentIntent(creator.create(this@toAndroidNotification)) }

//                TODO: Create Actions.
//                if (actions.isNotEmpty()) {
//                    for (action in actions) {
//                        notificationIntentCreators
//                            .firstOrNull { it.accept(this@toAndroidNotification) } ?: continue
//                    }
//                }
            }
            .build()
    }
}
