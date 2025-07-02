package net.thunderbird.feature.notification.api.content

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.notification_certificate_error_public
import net.thunderbird.feature.notification.resources.api.notification_certificate_error_text
import org.jetbrains.compose.resources.getString

/**
 * Notification for certificate errors.
 *
 * This notification is shown when there's an issue with a server's certificate,
 * preventing secure communication. It prompts the user to update their server settings.
 */
@ConsistentCopyVisibility
data class CertificateErrorNotification private constructor(
    override val id: NotificationId,
    override val title: String,
    override val contentText: String,
    val lockScreenTitle: String,
    override val channel: NotificationChannel,
) : AppNotification(), SystemNotification, InAppNotification {
    override val severity: NotificationSeverity = NotificationSeverity.Fatal
    override val actions: Set<NotificationAction> = setOf(NotificationAction.UpdateServerSettings)

    override val lockscreenNotification: SystemNotification get() = copy(
        contentText = lockScreenTitle,
    )

    companion object {
        /**
         * Creates a [CertificateErrorNotification].
         *
         * @param id The unique identifier for this notification.
         * @param accountUuid The UUID of the account associated with the notification.
         * @param accountDisplayName The display name of the account associated with the notification.
         * @return A [CertificateErrorNotification] instance.
         */
        suspend operator fun invoke(
            id: NotificationId,
            accountUuid: String,
            accountDisplayName: String,
        ): CertificateErrorNotification = CertificateErrorNotification(
            id = id,
            title = getString(resource = Res.string.notification_certificate_error_public, accountDisplayName),
            lockScreenTitle = getString(resource = Res.string.notification_certificate_error_public),
            contentText = getString(resource = Res.string.notification_certificate_error_text),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
        )
    }
}
