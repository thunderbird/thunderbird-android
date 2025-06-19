package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.common.io.KmpIgnoredOnParcel
import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.icon.FailedToCreate
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_notify_error_text
import net.thunderbird.feature.notification.resources.notification_notify_error_title
import org.jetbrains.compose.resources.getString

/**
 * Represents a notification indicating that the creation of another notification has failed.
 *
 * This notification is displayed both as a system notification and an in-app notification.
 * It has a critical severity level.
 */
@ConsistentCopyVisibility
@KmpParcelize
data class FailedToCreateNotification private constructor(
    override val accountNumber: Int,
    override val title: String,
    override val contentText: String?,
    override val channel: NotificationChannel,
    val failedNotification: AppNotification,
    @KmpIgnoredOnParcel
    override val icon: NotificationIcon = NotificationIcons.FailedToCreate,
) : AppNotification(), SystemNotification, InAppNotification {
    @KmpIgnoredOnParcel
    override val severity: NotificationSeverity = NotificationSeverity.Critical

    companion object {
        /**
         * Creates a [FailedToCreateNotification] instance.
         *
         * @param accountUuid The UUID of the account associated with the failed notification.
         * @param failedNotification The original [AppNotification] that failed to be created.
         * @return A [FailedToCreateNotification] instance.
         */
        suspend operator fun invoke(
            accountNumber: Int,
            accountUuid: String,
            failedNotification: AppNotification,
        ): FailedToCreateNotification = FailedToCreateNotification(
            accountNumber = accountNumber,
            title = getString(resource = Res.string.notification_notify_error_title),
            contentText = getString(resource = Res.string.notification_notify_error_text),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
            failedNotification = failedNotification,
        )
    }
}
