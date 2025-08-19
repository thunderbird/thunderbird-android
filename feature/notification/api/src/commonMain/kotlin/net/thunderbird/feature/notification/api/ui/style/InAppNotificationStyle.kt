package net.thunderbird.feature.notification.api.ui.style

import net.thunderbird.feature.notification.api.ui.style.builder.InAppNotificationStyleBuilder

/**
 * Represents the style of an in-app notification.
 *
 * In-app notifications are displayed within the application itself to provide immediate
 * feedback or information.
 */
sealed interface InAppNotificationStyle {
    companion object {
        /**
         * Represents an undefined in-app notification style.
         * This can be used as a default or placeholder when no specific style is applicable.
         */
        val Undefined: List<InAppNotificationStyle> = emptyList()
    }

    /**
     * @see InAppNotificationStyleBuilder.bannerInline
     */
    data object BannerInlineNotification : InAppNotificationStyle

    /**
     * @see InAppNotificationStyleBuilder.bannerGlobal
     */
    data class BannerGlobalNotification(
        val priority: Int,
    ) : InAppNotificationStyle

    /**
     * @see [InAppNotificationStyleBuilder.snackbar]
     */
    data class SnackbarNotification(
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : InAppNotificationStyle

    /**
     * @see [InAppNotificationStyleBuilder.dialog]
     */
    data object DialogNotification : InAppNotificationStyle
}

enum class SnackbarDuration { Short, Long, Indefinite }

/**
 * Configures the in-app notification style.
 *
 * Example:
 * ```
 * inAppNotificationStyles {
 *     snackbar(duration = 30.seconds)
 *     bottomSheet()
 * }
 * ```
 *
 * @param builder A lambda function with [InAppNotificationStyleBuilder] as its receiver,
 * used to configure the system notification style.
 * @return a list of [InAppNotificationStyle]
 */
@NotificationStyleMarker
fun inAppNotificationStyles(
    builder: @NotificationStyleMarker InAppNotificationStyleBuilder.() -> Unit,
): List<InAppNotificationStyle> {
    return InAppNotificationStyleBuilder().apply(builder).build()
}
