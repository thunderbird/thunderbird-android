package net.thunderbird.feature.notification.api.ui.builder

import net.thunderbird.feature.notification.api.ui.NotificationStyle
import net.thunderbird.feature.notification.api.ui.NotificationStyles

/**
 * Builder for creating notification styles for both system and in-app notifications.
 *
 * This builder allows for a declarative way to define the appearance and behavior of notifications.
 * It uses a Kotlin DSL approach for configuring the styles.
 *
 * Example usage:
 * ```
 * val (systemStyle, inAppStyle) = notificationStyle {
 *     systemStyle {
 *         bigText("This is a big text notification.")
 *     }
 *     inAppStyle {
 *         // Configure in-app notification style
 *     }
 * }
 * ```
 */
class NotificationStyleBuilder {
    private var systemNotificationStyle: NotificationStyle.System = NotificationStyle.System.Undefined
    private var inAppNotificationStyle: NotificationStyle.InApp = NotificationStyle.InApp.Undefined

    /**
     * Configures the system notification style.
     *
     * @param builder A lambda function with [SystemNotificationStyleBuilder] as its receiver,
     * used to configure the system notification style.
     *
     * Example:
     * ```
     * systemStyle {
     *     bigText("This is a big text notification.")
     *     // or
     *     inbox {
     *         // Add more inbox style configurations here
     *     }
     * }
     * ```
     */
    @NotificationStyleMarker
    fun systemStyle(builder: @NotificationStyleMarker SystemNotificationStyleBuilder.() -> Unit) {
        systemNotificationStyle = SystemNotificationStyleBuilder().apply(builder).build()
    }

    @NotificationStyleMarker
    fun inAppStyle(builder: @NotificationStyleMarker InAppNotificationStyleBuilder.() -> Unit) {
        inAppNotificationStyle = InAppNotificationStyleBuilder().apply(builder).build()
    }

    /**
     * Builds and returns the configured system and in-app notification styles.
     *
     * This function should be called after all desired configurations have been applied
     * using the `systemStyle` and `inAppStyle` DSL blocks.
     *
     * @return A [Pair] containing the [NotificationStyle.System] and [NotificationStyle.InApp].
     *         If a style was not explicitly configured, it will default to its `Undefined` state.
     *
     * @see notificationStyle
     * @see NotificationStyle.System.Undefined
     * @see NotificationStyle.InApp.Undefined
     */
    fun build(): NotificationStyles = NotificationStyles(
        systemStyle = systemNotificationStyle,
        inAppStyle = inAppNotificationStyle,
    )
}

/**
 * DSL entry point for creating notification styles.
 *
 * This function provides a convenient way to build both system and in-app notification
 * styles using a Kotlin DSL.
 *
 * Example usage:
 * ```
 * val (systemStyle, inAppStyle) = notificationStyle {
 *     systemStyle {
 *         bigText("This is a big text notification.")
 *     }
 *     inAppStyle {
 *         // Configure in-app notification style
 *     }
 * }
 * ```
 *
 * @param builder A lambda expression that configures the notification styles using the
 *  [NotificationStyleBuilder] DSL.
 * @return A [Pair] containing the configured [NotificationStyle.System] and
 *  [NotificationStyle.InApp] styles.
 */
@NotificationStyleMarker
fun notificationStyle(
    builder: @NotificationStyleMarker NotificationStyleBuilder.() -> Unit,
): NotificationStyles {
    return NotificationStyleBuilder().apply(builder).build()
}
