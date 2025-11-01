package net.thunderbird.feature.notification.impl.intent.action

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import androidx.core.app.PendingIntentCompat
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.notification.api.content.AuthenticationErrorNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction

private const val TAG = "UpdateServerSettingsNotificationActionIntentCreator"

class UpdateServerSettingsNotificationActionIntentCreator(
    private val context: Context,
    private val logger: Logger,
) : NotificationActionIntentCreator<AuthenticationErrorNotification, NotificationAction> {
    override fun accept(notification: Notification, action: NotificationAction): Boolean =
        notification is AuthenticationErrorNotification &&
            (
                action is NotificationAction.UpdateIncomingServerSettings ||
                    action is NotificationAction.UpdateOutgoingServerSettings
                )

    override fun create(
        notification: AuthenticationErrorNotification,
        action: NotificationAction,
    ): PendingIntent? {
        val (accountNumber, intent) = when (action) {
            is NotificationAction.UpdateIncomingServerSettings -> {
                action.accountNumber to FeatureLauncherActivity.getIntent(
                    context = context,
                    target = FeatureLauncherTarget.AccountEditIncomingSettings(action.accountUuid),
                )
            }

            is NotificationAction.UpdateOutgoingServerSettings -> {
                action.accountNumber to FeatureLauncherActivity.getIntent(
                    context = context,
                    target = FeatureLauncherTarget.AccountEditOutgoingSettings(action.accountUuid),
                )
            }

            else -> error("Unsupported action: $action")
        }

        return PendingIntentCompat.getActivity(
            context,
            accountNumber,
            intent,
            FLAG_UPDATE_CURRENT,
            false,
        )
    }
}
