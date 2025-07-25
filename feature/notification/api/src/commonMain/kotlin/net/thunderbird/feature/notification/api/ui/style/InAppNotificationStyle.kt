package net.thunderbird.feature.notification.api.ui.style

import net.thunderbird.feature.notification.api.ui.style.builder.InAppNotificationStyleBuilder

/**
 * Represents the style of an in-app notification.
 *
 * In-app notifications are displayed within the application itself to provide immediate
 * feedback or information.
 *
 * TODO(#9312): The subtypes of [InAppNotificationStyle] Style might change after designer's feedback.
 */
enum class InAppNotificationStyle {
    /**
     * Represents an undefined in-app notification style.
     * This can be used as a default or placeholder when no specific style is applicable.
     */
    Undefined,

    /**
     * Represents a fatal error notification that cannot be dismissed by the user.
     *
     * This type of notification typically indicates a fatal issue that requires user attention
     * and prevents normal operation of the application.
     */
    Fatal,

    /**
     * Represents a critical in-app notification style.
     *
     * This style is used for important messages that require user attention but do not
     * necessarily halt the application's functionality like a [Fatal] error.
     */
    Critical,

    /**
     * Represents a temporary in-app notification style.
     *
     * This style is typically used for notifications that are displayed briefly and then dismissed
     * automatically or by user interaction.
     */
    Temporary,

    /**
     * Represents a general warning notification.
     */
    Warning,

    /**
     * Represents an in-app notification that displays general information.
     *
     * This style is typically used for notifications that convey important updates or messages
     * that don't fit into more specific categories like errors or successes.
     */
    Information,
}

/**
 * Configures the in-app notification style.
 *
 * @param builder A lambda function with [InAppNotificationStyleBuilder] as its receiver,
 * used to configure the system notification style.
 *
 * Example:
 * ```
 * inAppNotificationStyle {
 *     severity(NotificationSeverity.Fatal)
 * }
 * ```
 *
 * TODO(#9312): The subtypes of [InAppNotificationStyle] Style might change after designer's feedback.
 */
@NotificationStyleMarker
fun inAppNotificationStyle(
    builder: @NotificationStyleMarker InAppNotificationStyleBuilder.() -> Unit,
): InAppNotificationStyle {
    return InAppNotificationStyleBuilder().apply(builder).build()
}
