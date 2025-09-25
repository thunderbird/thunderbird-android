package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.icon.AuthenticationError
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.api.ui.style.inAppNotificationStyle
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.notification_authentication_error_title
import net.thunderbird.feature.notification.resources.api.notification_authentication_incoming_server_error_text
import net.thunderbird.feature.notification.resources.api.notification_authentication_outgoing_server_error_text
import org.jetbrains.compose.resources.getString

/**
 * Notification to be displayed when an authentication error occurs.
 *
 * This notification is both a [SystemNotification] and an [InAppNotification].
 */
@ConsistentCopyVisibility
data class AuthenticationErrorNotification private constructor(
    val isIncomingServerError: Boolean,
    override val accountUuid: String,
    val accountNumber: Int,
    override val title: String,
    override val contentText: String?,
    override val channel: NotificationChannel,
) : AppNotification(), SystemNotification, InAppNotification {
    override val icon: NotificationIcon = NotificationIcons.AuthenticationError
    override val severity: NotificationSeverity = NotificationSeverity.Fatal
    override val actions: Set<NotificationAction> = buildSet {
        val action = if (isIncomingServerError) {
            NotificationAction.UpdateIncomingServerSettings(accountUuid, accountNumber)
        } else {
            NotificationAction.UpdateOutgoingServerSettings(accountUuid, accountNumber)
        }
        add(action)
        add(NotificationAction.Tap(override = action))
    }
    override val inAppNotificationStyle = inAppNotificationStyle { bannerInline() }

    override fun asLockscreenNotification(): SystemNotification.LockscreenNotification =
        SystemNotification.LockscreenNotification(
            notification = copy(contentText = null),
        )

    companion object {
        /**
         * Creates an [AuthenticationErrorNotification].
         *
         * @param accountUuid The UUID of the account associated with the authentication error.
         * @param accountDisplayName The display name of the account associated with the authentication error.
         * @return An [AuthenticationErrorNotification] instance.
         */
        suspend operator fun invoke(
            accountUuid: String,
            accountDisplayName: String,
            accountNumber: Int,
            isIncomingServerError: Boolean,
        ): AuthenticationErrorNotification = AuthenticationErrorNotification(
            isIncomingServerError = isIncomingServerError,
            accountUuid = accountUuid,
            accountNumber = accountNumber,
            title = getString(resource = Res.string.notification_authentication_error_title),
            contentText = getString(
                resource = if (isIncomingServerError) {
                    Res.string.notification_authentication_incoming_server_error_text
                } else {
                    Res.string.notification_authentication_outgoing_server_error_text
                },
                accountDisplayName,
            ),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
        )
    }
}
