package net.thunderbird.feature.notification.impl.intent.action

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.PendingIntentCompat
import androidx.core.net.toUri
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.content.PushServiceNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

private const val TAG = "AlarmPermissionMissingNotificationIntentCreator"

class AlarmPermissionMissingNotificationTapActionIntentCreator(
    private val context: Context,
    private val logger: Logger,
) : NotificationActionIntentCreator<PushServiceNotification.AlarmPermissionMissing, NotificationAction.Tap> {
    override fun accept(notification: Notification, action: NotificationAction): Boolean =
        Build.VERSION.SDK_INT > Build.VERSION_CODES.S &&
            notification is PushServiceNotification.AlarmPermissionMissing

    @RequiresApi(Build.VERSION_CODES.S)
    override fun create(
        notification: PushServiceNotification.AlarmPermissionMissing,
        action: NotificationAction.Tap,
    ): PendingIntent {
        logger.debug(TAG) { "create() called with: notification = $notification" }
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:${context.packageName}".toUri()
        }

        return requireNotNull(
            PendingIntentCompat.getActivity(
                /* context = */
                context,
                /* requestCode = */
                1,
                /* intent = */
                intent,
                /* flags = */
                0,
                /* isMutable = */
                false,
            ),
        ) {
            "Could not create PendingIntent for AlarmPermissionMissing Notification."
        }
    }
}
