package net.thunderbird.feature.notification.api.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification

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

    /**
     * Resolves the [SystemNotificationIcon] for a given [SystemNotification].
     *
     * This function is used to retrieve the appropriate system notification icon
     * associated with this [NotificationIcon] instance.
     *
     * @param notification The [SystemNotification] for which to resolve the icon.
     * @return The [SystemNotificationIcon] associated with this notification.
     * @throws IllegalArgumentException if [systemNotificationIcon] is `null`, indicating
     * that this [NotificationIcon] instance was not configured for a system notification icon.
     */
    fun resolve(notification: SystemNotification): SystemNotificationIcon {
        return requireNotNull(systemNotificationIcon) {
            "$notification requires a not null SystemNotificationIcon."
        }
    }

    /**
     * Resolves the [ImageVector] for a given [InAppNotification].
     *
     * This function is used to retrieve the appropriate system notification icon
     * associated with this [NotificationIcon] instance.
     *
     * @param notification The [InAppNotification] for which to resolve the icon.
     * @return The [InAppNotification] associated with this notification.
     * @throws IllegalArgumentException if [inAppNotificationIcon] is `null`, indicating
     * that this [InAppNotification] instance was not configured for a in-app notification icon.
     */
    fun resolve(notification: InAppNotification): ImageVector {
        return requireNotNull(inAppNotificationIcon) {
            "$notification requires a not null InAppNotification."
        }
    }
}

/**
 * Represents an icon for a system notification.
 *
 * This is an expect class, meaning its actual implementation is provided by platform-specific modules.
 * On Android, this would typically wrap a drawable resource ID.
 * On other platforms, it might represent a file path or another platform-specific icon identifier.
 */
expect class SystemNotificationIcon
