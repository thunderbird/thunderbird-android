package net.thunderbird.feature.notification.api.ui.builder

/**
 * A DSL marker for building notification styles.
 *
 * This annotation is used to restrict the scope of lambda receivers, ensuring that
 * methods belonging to an outer scope cannot be called from an inner scope.
 * This helps in creating a more structured and type-safe DSL for constructing
 * different notification styles.
 *
 * For example, when defining a `systemStyle` configuration within a `NotificationStyleBuilder` scope,
 * methods specific to the `NotificationStyleBuilder` should not be directly callable
 * from the `systemStyle`'s configuration block.
 *
 * Example:
 * ```
 * // OK:
 * val (systemStyle, inAppStyle) = notificationStyle {
 *     systemStyle {
 *         bigText("This is a big text notification.")
 *     }
 *     inAppStyle {
 *         // Configure in-app notification style
 *     }
 * }
 *
 * // OK, but discouraged:
 * val (systemStyle, inAppStyle) = notificationStyle {
 *     systemStyle {
 *         bigText("This is a big text notification.")
 *         this@notificationStyle.inAppStyle {
 *             // inAppStyle must be called within notificationStyle and not within systemStyle.
 *         }
 *     }
 * }
 *
 * // Compile error:
 * val (systemStyle, inAppStyle) = notificationStyle {
 *     systemStyle {
 *         bigText("This is a big text notification.")
 *         inAppStyle {
 *             // inAppStyle must be called within notificationStyle and not within systemStyle.
 *         }
 *     }
 * }
 * ```
 */
@DslMarker
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION)
annotation class NotificationStyleMarker
