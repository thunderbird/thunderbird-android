package net.thunderbird.feature.notification.api.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents the icon to be displayed for a notification.
 *
 * This class allows specifying different icons for system notifications and in-app notifications.
 * At least one type of icon must be provided.
 *
 * @property systemNotificationIcon The icon to be used for system notifications.
 * @property inAppNotificationIcon The icon to be used for in-app notifications.
 */
data class NotificationIcon(
    private val systemNotificationIcon: SystemNotificationIcon? = null,
    private val inAppNotificationIcon: ImageVector? = null,
) {

    init {
        check(systemNotificationIcon != null || inAppNotificationIcon != null) {
            "Both systemNotificationIcon and inAppNotificationIcon are null. " +
                "You must specify at least one type of icon."
        }
    }
}
