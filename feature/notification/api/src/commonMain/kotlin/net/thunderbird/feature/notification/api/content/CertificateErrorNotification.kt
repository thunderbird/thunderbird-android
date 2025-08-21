package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.icon.CertificateError
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.notification_certificate_error_public
import net.thunderbird.feature.notification.resources.api.notification_certificate_error_text
import net.thunderbird.feature.notification.resources.api.notification_certificate_error_title
import org.jetbrains.compose.resources.getString

/**
 * Notification for certificate errors.
 *
 * This notification is shown when there's an issue with a server's certificate,
 * preventing secure communication. It prompts the user to update their server settings.
 */
@ConsistentCopyVisibility
data class CertificateErrorNotification private constructor(
    val isIncomingServerError: Boolean,
    override val accountUuid: String,
    override val title: String,
    override val contentText: String,
    val lockScreenTitle: String,
    override val channel: NotificationChannel,
    override val icon: NotificationIcon = NotificationIcons.CertificateError,
) : AppNotification(), SystemNotification, InAppNotification {
    override val severity: NotificationSeverity = NotificationSeverity.Fatal
    override val actions: Set<NotificationAction> = setOf(
        if (isIncomingServerError) {
            NotificationAction.UpdateIncomingServerSettings(accountUuid)
        } else {
            NotificationAction.UpdateOutgoingServerSettings(accountUuid)
        },
    )

    override fun asLockscreenNotification(): SystemNotification.LockscreenNotification =
        SystemNotification.LockscreenNotification(
            notification = copy(contentText = lockScreenTitle),
        )

    companion object {
        /**
         * Creates a [CertificateErrorNotification].
         *
         * @param accountUuid The UUID of the account associated with the notification.
         * @param accountDisplayName The display name of the account associated with the notification.
         * @return A [CertificateErrorNotification] instance.
         */
        suspend operator fun invoke(
            accountUuid: String,
            accountDisplayName: String,
            isIncomingServerError: Boolean,
        ): CertificateErrorNotification = CertificateErrorNotification(
            isIncomingServerError = isIncomingServerError,
            accountUuid = accountUuid,
            title = getString(resource = Res.string.notification_certificate_error_title, accountDisplayName),
            lockScreenTitle = getString(resource = Res.string.notification_certificate_error_public),
            contentText = getString(resource = Res.string.notification_certificate_error_text),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
        )
    }
}
