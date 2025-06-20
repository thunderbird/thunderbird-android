package net.thunderbird.feature.notification.api.ui

import org.jetbrains.annotations.VisibleForTesting

/**
 * Represents the different styles a notification can have, catering to both system-level
 * and in-app display formats.
 */
sealed interface NotificationStyle {
    /**
     * Represents the style of a system notification.
     */
    sealed interface System : NotificationStyle {
        /**
         * Represents an undefined notification style.
         * This can be used as a default or placeholder when no specific style is applicable.
         */
        data object Undefined : System

        /**
         * Style for large-format notifications that include a lot of text.
         *
         * @property text The main text content of the notification.
         */
        data class BigTextStyle @VisibleForTesting constructor(
            val text: String,
        ) : System

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
        ) : System
    }

    /**
     * Represents the style of an in-app notification.
     *
     * In-app notifications are displayed within the application itself to provide immediate
     * feedback or information.
     *
     * TODO: The subtypes of [InApp] Style might change after designer's feedback.
     */
    enum class InApp : NotificationStyle {
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
}

/**
 * Holds the specific styles for both system-level and in-app notifications.
 *
 * This allows for defining distinct visual and behavioral characteristics for notifications
 * depending on whether they are displayed by the operating system or within the application.
 *
 * @property systemStyle The style to be applied for system notifications.
 * @property inAppStyle The style to be applied for in-app notifications.
 */
@ConsistentCopyVisibility
data class NotificationStyles internal constructor(
    val systemStyle: NotificationStyle.System,
    val inAppStyle: NotificationStyle.InApp,
)
