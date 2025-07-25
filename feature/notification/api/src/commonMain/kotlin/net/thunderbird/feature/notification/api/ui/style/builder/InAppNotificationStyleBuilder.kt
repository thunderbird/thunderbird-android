package net.thunderbird.feature.notification.api.ui.style.builder

import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle

/**
 * Builder for creating [InAppNotificationStyle] instances.
 * This interface defines the methods available for configuring the style of an in-app notification.
 */
class InAppNotificationStyleBuilder internal constructor() {
    private var style = InAppNotificationStyle.Undefined

    /**
     * Sets the severity of the in-app notification.
     *
     * @param severity The severity level for the notification.
     */
    fun severity(severity: NotificationSeverity) {
        require(style == InAppNotificationStyle.Undefined) {
            "In-App Notifications must have only one severity."
        }
        style = when (severity) {
            NotificationSeverity.Fatal -> InAppNotificationStyle.Fatal
            NotificationSeverity.Critical -> InAppNotificationStyle.Critical
            NotificationSeverity.Temporary -> InAppNotificationStyle.Temporary
            NotificationSeverity.Warning -> InAppNotificationStyle.Warning
            NotificationSeverity.Information -> InAppNotificationStyle.Information
        }
    }

    /**
     * Builds the [InAppNotificationStyle] based on the provided parameters.
     *
     * @return The constructed [InAppNotificationStyle].
     */
    internal fun build(): InAppNotificationStyle {
        check(style != InAppNotificationStyle.Undefined) {
            "You must add severity of the in-app notification."
        }
        return style
    }
}
