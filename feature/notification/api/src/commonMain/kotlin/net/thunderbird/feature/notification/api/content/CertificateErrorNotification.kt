package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.common.io.KmpIgnoredOnParcel
import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_certificate_error_public
import net.thunderbird.feature.notification.resources.notification_certificate_error_text
import org.jetbrains.compose.resources.getString

/**
 * Notification for certificate errors.
 *
 * This notification is shown when there's an issue with a server's certificate,
 * preventing secure communication. It prompts the user to update their server settings.
 */
@ConsistentCopyVisibility
@KmpParcelize
data class CertificateErrorNotification private constructor(
    override val accountNumber: Int,
    override val title: String,
    override val contentText: String,
    val lockScreenTitle: String,
    override val channel: NotificationChannel,
) : AppNotification(), SystemNotification, InAppNotification {
    @KmpIgnoredOnParcel
    override val severity: NotificationSeverity = NotificationSeverity.Fatal

    @KmpIgnoredOnParcel
    override val actions: Set<NotificationAction> = setOf(NotificationAction.UpdateServerSettings)

    override val lockscreenNotification: SystemNotification get() = copy(
        contentText = lockScreenTitle,
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
            accountNumber: Int,
            accountUuid: String,
            accountDisplayName: String,
        ): CertificateErrorNotification = CertificateErrorNotification(
            accountNumber = accountNumber,
            title = getString(resource = Res.string.notification_certificate_error_public, accountDisplayName),
            lockScreenTitle = getString(resource = Res.string.notification_certificate_error_public),
            contentText = getString(resource = Res.string.notification_certificate_error_text),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
        )
    }
}
