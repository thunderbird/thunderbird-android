package net.thunderbird.feature.notification.api.ui.style

/**
 * A DSL marker for building notification styles.
 *
 * This annotation is used to restrict the scope of lambda receivers, ensuring that
 * methods belonging to an outer scope cannot be called from an inner scope.
 * This helps in creating a more structured and type-safe DSL for constructing
 * different notification styles.
 *
 * Example:
 * ```
 * // OK:
 * val systemStyle = systemNotificationStyle {
 *     bigText("This is a big text notification.")
 * }
 *
 * // Compile error:
 * val systemStyle = systemNotificationStyle {
 *     inbox {
 *         // bigText must be called within systemNotificationStyle and not within inbox configuration.
 *         bigText("This is a big text notification.")
 *     }
 * }
 * ```
 */
@DslMarker
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
internal annotation class NotificationStyleMarker
