package net.thunderbird.feature.notification.api.ui.style.builder

import kotlin.apply
import net.thunderbird.feature.notification.api.ui.style.NotificationStyleMarker
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle.BigTextStyle
import net.thunderbird.feature.notification.api.ui.style.SystemNotificationStyle.InboxStyle

/**
 * A builder for creating system notification styles.
 *
 * This builder allows for the creation of either a [BigTextStyle] or an [InboxStyle] for a system notification.
 * It ensures that only one style type is set at a time, throwing an error if both are attempted.
 *
 * Example usage for [BigTextStyle]:
 * ```
 * val style = systemNotificationStyle {
 *     bigText("This is a long piece of text that will be displayed in the expanded notification.")
 * }
 * ```
 *
 * Example usage for [InboxStyle]:
 * ```
 * val style = systemNotificationStyle {
 *     inbox {
 *         title("5 New Messages")
 *         summary("You have new messages")
 *         addLine("Alice: Hey, are you free later?")
 *         addLine("Bob: Meeting reminder for 3 PM")
 *     }
 * }
 * ```
 * @see net.thunderbird.feature.notification.api.ui.style.systemNotificationStyle
 */
class SystemNotificationStyleBuilder internal constructor() {
    private var bigText: BigTextStyle? = null
    private var inboxStyle: InboxStyle? = null

    /**
     * Sets the style of the notification to [SystemNotificationStyle.BigTextStyle].
     *
     * This style displays a large block of text.
     *
     * **Note:** A system notification can either have a BigText or InboxStyle, not both.
     *
     * @param text The text to be displayed in the notification.
     */
    fun bigText(text: String) {
        @Suppress("VisibleForTests")
        bigText = BigTextStyle(text = text)
    }

    /**
     * Sets the style of the notification to [SystemNotificationStyle.InboxStyle].
     *
     * This style is designed for aggregated notifications.
     *
     * **Note:** A system notification can either have a BigText or InboxStyle, not both.
     *
     * @param builder A lambda with [InboxSystemNotificationStyleBuilder] as its receiver,
     * used to configure the Inbox style.
     * @see InboxSystemNotificationStyleBuilder
     */
    @NotificationStyleMarker
    fun inbox(builder: @NotificationStyleMarker InboxSystemNotificationStyleBuilder.() -> Unit) {
        inboxStyle = InboxSystemNotificationStyleBuilder().apply(builder).build()
    }

    /**
     * Builds and returns the configured [SystemNotificationStyle].
     *
     * This method validates that either a [BigTextStyle] or an [InboxStyle] has been set, but not both.
     * If both styles are set, or if neither style is set (which should be an unexpected state),
     * it will throw an [IllegalStateException].
     *
     * @return The configured [SystemNotificationStyle] which will be either a [BigTextStyle] or an [InboxStyle].
     * @throws IllegalStateException if both `bigText` and `inboxStyle` are set, or if neither are set.
     */
    internal fun build(): SystemNotificationStyle {
        // shadowing properties to safely capture its value at the call time.
        val bigText = bigText
        val inboxStyle = inboxStyle
        return when {
            bigText != null && inboxStyle != null -> error(
                "A system notification can either have a BigText or InboxStyle, not both.",
            )

            bigText != null -> bigText

            inboxStyle != null -> inboxStyle

            else -> error("You must configure at least one of the following styles: bigText or inbox.")
        }
    }
}
