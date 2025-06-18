package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.common.io.KmpIgnoredOnParcel
import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_authentication_error_text
import net.thunderbird.feature.notification.resources.notification_authentication_error_title
import org.jetbrains.compose.resources.getString

/**
 * Notification to be displayed when an authentication error occurs.
 *
 * This notification is both a [SystemNotification] and an [InAppNotification].
 */
@ConsistentCopyVisibility
@KmpParcelize
data class AuthenticationErrorNotification private constructor(
    override val accountNumber: Int,
    override val title: String,
    override val contentText: String?,
    override val channel: NotificationChannel,
) : AppNotification(), SystemNotification, InAppNotification {
    @KmpIgnoredOnParcel
    override val severity: NotificationSeverity = NotificationSeverity.Fatal

    @KmpIgnoredOnParcel
    override val actions: Set<NotificationAction> = setOf(
        NotificationAction.Retry,
        NotificationAction.UpdateServerSettings,
    )

    override val lockscreenNotification: SystemNotification get() = copy(contentText = null)

    companion object {
        /**
         * Creates an [AuthenticationErrorNotification].
         *
         * @param accountUuid The UUID of the account associated with the authentication error.
         * @param accountDisplayName The display name of the account associated with the authentication error.
         * @return An [AuthenticationErrorNotification] instance.
         */
        suspend operator fun invoke(
            accountNumber: Int,
            accountUuid: String,
            accountDisplayName: String,
        ): AuthenticationErrorNotification = AuthenticationErrorNotification(
            accountNumber = accountNumber,
            title = getString(resource = Res.string.notification_authentication_error_title),
            contentText = getString(
                resource = Res.string.notification_authentication_error_text,
                accountDisplayName,
            ),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
        )
    }
}
