package net.thunderbird.feature.notification.content

import net.thunderbird.feature.notification.NotificationChannel
import net.thunderbird.feature.notification.NotificationId
import net.thunderbird.feature.notification.NotificationSeverity
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
data class FailedToCreateNotification private constructor(
    override val id: NotificationId,
    override val title: String,
    override val contentText: String?,
    override val channel: NotificationChannel,
    val failedNotification: AppNotification,
) : AppNotification(), SystemNotification, InAppNotification {
    override val severity: NotificationSeverity = NotificationSeverity.Critical

    companion object {
        /**
         * Creates a [FailedToCreateNotification] instance.
         *
         * @param id The unique identifier for this notification.
         * @param accountUuid The UUID of the account associated with the failed notification.
         * @param failedNotification The original [AppNotification] that failed to be created.
         * @return A [FailedToCreateNotification] instance.
         */
        suspend operator fun invoke(
            id: NotificationId,
            accountUuid: String,
            failedNotification: AppNotification,
        ): FailedToCreateNotification = FailedToCreateNotification(
            id = id,
            title = getString(resource = Res.string.notification_notify_error_title),
            contentText = getString(resource = Res.string.notification_notify_error_text),
            channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
            failedNotification = failedNotification,
        )
    }
}
