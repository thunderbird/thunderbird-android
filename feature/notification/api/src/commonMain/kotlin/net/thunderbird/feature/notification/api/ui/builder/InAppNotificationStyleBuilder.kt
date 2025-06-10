package net.thunderbird.feature.notification.api.ui.builder

import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.NotificationStyle

/**
 * Builder for creating [NotificationStyle.InApp] instances.
 * This interface defines the methods available for configuring the style of an in-app notification.
 */
class InAppNotificationStyleBuilder internal constructor() {
    private var style = NotificationStyle.InApp.Undefined

    /**
     * Sets the severity of the in-app notification.
     *
     * @param severity The severity level for the notification.
     */
    fun severity(severity: NotificationSeverity) {
        require(style == NotificationStyle.InApp.Undefined) {
            "In-App Notifications must have only one severity."
        }
        style = when (severity) {
            NotificationSeverity.Fatal -> NotificationStyle.InApp.Fatal
            NotificationSeverity.Critical -> NotificationStyle.InApp.Critical
            NotificationSeverity.Temporary -> NotificationStyle.InApp.Temporary
            NotificationSeverity.Warning -> NotificationStyle.InApp.Warning
            NotificationSeverity.Information -> NotificationStyle.InApp.Information
        }
    }

    /**
     * Builds the [NotificationStyle.InApp] based on the provided parameters.
     *
     * @return The constructed [NotificationStyle.InApp].
     */
    fun build(): NotificationStyle.InApp {
        check(style != NotificationStyle.InApp.Undefined) {
            "You must add severity of the in-app notification."
        }
        return style
    }
}
