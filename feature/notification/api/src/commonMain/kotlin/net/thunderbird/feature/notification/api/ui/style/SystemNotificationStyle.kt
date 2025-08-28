package net.thunderbird.feature.notification.api.ui.style

import net.thunderbird.feature.notification.api.ui.style.builder.SystemNotificationStyleBuilder
import org.jetbrains.annotations.VisibleForTesting

/**
 * Represents the style of a system notification.
 */
sealed interface SystemNotificationStyle {
    /**
     * Represents an undefined notification style.
     * This can be used as a default or placeholder when no specific style is applicable.
     */
    data object Undefined : SystemNotificationStyle

    /**
     * Style for large-format notifications that include a lot of text.
     *
     * @property text The main text content of the notification.
     */
    data class BigTextStyle @VisibleForTesting constructor(
        val text: String,
    ) : SystemNotificationStyle

    /**
     * Style for large-format notifications that include a list of (up to 5) strings.
     *
     * @property bigContentTitle Overrides the title of the notification.
     * @property summary Overrides the summary of the notification.
     * @property lines List of strings to display in the notification.
     */
    data class InboxStyle @VisibleForTesting constructor(
        val bigContentTitle: String,
        val summary: String,
        val lines: List<CharSequence>,
    ) : SystemNotificationStyle
}

/**
 * Configures the system notification style.
 *
 * @param builder A lambda function with [SystemNotificationStyleBuilder] as its receiver,
 * used to configure the system notification style.
 *
 * Example:
 * ```
 * systemNotificationStyle {
 *     bigText("This is a big text notification.")
 *     // or
 *     inbox {
 *         // Add more inbox style configurations here
 *     }
 * }
 * ```
 */
@NotificationStyleMarker
fun systemNotificationStyle(
    builder: @NotificationStyleMarker SystemNotificationStyleBuilder.() -> Unit,
): SystemNotificationStyle {
    return SystemNotificationStyleBuilder().apply(builder).build()
}
