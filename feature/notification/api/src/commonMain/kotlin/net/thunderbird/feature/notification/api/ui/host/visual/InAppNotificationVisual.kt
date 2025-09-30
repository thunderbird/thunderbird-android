package net.thunderbird.feature.notification.api.ui.host.visual

import androidx.compose.runtime.Stable
import kotlin.time.Duration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual.Companion.MAX_SUPPORTING_TEXT_LENGTH
import net.thunderbird.feature.notification.api.ui.host.visual.BannerInlineVisual.Companion.MAX_TITLE_LENGTH
import net.thunderbird.feature.notification.api.ui.style.InAppNotificationStyle
import net.thunderbird.feature.notification.api.ui.style.SnackbarDuration

sealed interface InAppNotificationVisual

/**
 * Represents the visual appearance of a [InAppNotificationStyle.BannerGlobalNotification].
 *
 * @property message The text content to be displayed in the banner global. This can be any CharSequence,
 *   allowing for formatted text.
 * @property severity The [NotificationSeverity] of the notification, indicating its importance or type.
 *   Used to determine which BannerGlobal visual style to use.
 * @property action An optional [NotificationAction] that the user can perform in response to the notification.
 *   If null, no action is available.
 * @property priority An integer representing the priority of the notification. Higher values typically indicate
 *   higher priority. Used to determine which notification to display, in case of multiples
 *   [InAppNotificationStyle.BannerGlobalNotification].
 * @see InAppNotificationVisual
 * @see InAppNotificationStyle.BannerGlobalNotification
 */
@Stable
data class BannerGlobalVisual(
    val message: CharSequence,
    val severity: NotificationSeverity,
    val action: NotificationAction?,
    val priority: Int,
) : InAppNotificationVisual {
    internal companion object {
        /**
         * Creates a [BannerGlobalVisual] from an [InAppNotification].
         *
         * This function attempts to convert an [InAppNotification] into a [BannerGlobalVisual].
         * It expects the notification to have a style of [InAppNotificationStyle.BannerGlobalNotification].
         *
         * It performs the following checks:
         * - The `contentText` of the notification must not be null.
         * - The notification must have zero or one action.
         *
         * If the notification has a matching style and passes all checks, a [BannerGlobalVisual] is created.
         * Otherwise, this function returns `null`.
         *
         * @param notification The [InAppNotification] to convert.
         * @return A [BannerGlobalVisual] if the conversion is successful, `null` otherwise.
         * @throws IllegalStateException fails the check validations.
         */
        fun from(notification: InAppNotification): BannerGlobalVisual? =
            notification.toVisual<InAppNotificationStyle.BannerGlobalNotification, BannerGlobalVisual> { style ->
                BannerGlobalVisual(
                    message = checkNotNull(notification.contentText) {
                        "A notification with a BannerGlobalNotification style must have a contentText not null"
                    },
                    severity = notification.severity,
                    action = notification
                        .actionsWithoutTap
                        .let { actions ->
                            check(actions.size in 0..1) {
                                "A notification with a BannerGlobalNotification style must have at zero or one action"
                            }
                            actions.toPersistentList()
                        }
                        .firstOrNull(),
                    priority = style.priority,
                )
            }
    }
}

/**
 * Represents the visual appearance of a [InAppNotificationStyle.BannerInlineNotification].
 *
 * @property title The title of the notification.
 * @property supportingText The main content/message of the notification.
 * @property severity The [NotificationSeverity] of the notification, indicating its importance or type.
 *   Used to determine which BannerGlobal visual style to use.
 * @property actions An immutable list of [NotificationAction] objects representing actions
 *  the user can take in response to the notification.
 * @see InAppNotificationVisual
 * @see InAppNotificationStyle.BannerInlineNotification
 */
@Stable
data class BannerInlineVisual(
    val title: CharSequence,
    val supportingText: CharSequence,
    val severity: NotificationSeverity,
    val actions: ImmutableList<NotificationAction>,
) : InAppNotificationVisual {
    companion object {
        internal const val MAX_TITLE_LENGTH = 100
        internal const val MAX_SUPPORTING_TEXT_LENGTH = 200

        /**
         * Creates a [BannerInlineVisual] from an [InAppNotification].
         *
         * This function attempts to convert an [InAppNotification] into a [BannerInlineVisual].
         * It expects the notification to have a style of [InAppNotificationStyle.BannerInlineNotification].
         *
         * It performs the following checks:
         * - The `title` of the notification must have at least 1 and at most [MAX_TITLE_LENGTH] chars.
         * - The `contentText` of the notification must not be null and have at least 1 and at most
         *   [MAX_SUPPORTING_TEXT_LENGTH] chars.
         * - The notification must have 1 or 2 actions.
         *
         * If the notification has a matching style and passes all checks, a [BannerInlineVisual] is created.
         * Otherwise, this function returns an empty list.
         *
         * @param notification The [InAppNotification] to convert.
         * @return A list containing a [BannerInlineVisual] if the conversion is successful, an empty list otherwise.
         * @throws IllegalStateException if any of the validation checks fail.
         */
        fun from(notification: InAppNotification): BannerInlineVisual? =
            notification.toVisual<InAppNotificationStyle.BannerInlineNotification, BannerInlineVisual> { style ->
                BannerInlineVisual(
                    title = checkTitle(notification.title),
                    supportingText = checkContentText(notification.contentText),
                    severity = notification.severity,
                    actions = notification
                        .actionsWithoutTap
                        .let { actions ->
                            check(actions.size in 1..2) {
                                "A notification with a BannerInlineNotification style must have at one or two actions"
                            }
                            actions.toPersistentList()
                        },
                )
            }

        private fun checkTitle(title: String): String {
            check(title.length in 1..MAX_TITLE_LENGTH) {
                "A notification with a BannerInlineNotification style must have a title length of 1 to " +
                    "$MAX_TITLE_LENGTH characters."
            }
            return title
        }

        private fun checkContentText(contentText: String?): String {
            checkNotNull(contentText) {
                "A notification with a BannerInlineNotification style must have a contentText not null"
            }
            check(contentText.length in 1..MAX_SUPPORTING_TEXT_LENGTH) {
                "A notification with a BannerInlineNotification style must have a contentText length of 1 to " +
                    "$MAX_SUPPORTING_TEXT_LENGTH characters."
            }
            return contentText
        }
    }
}

/**
 * Represents the visual appearance of a [InAppNotificationStyle.SnackbarNotification].
 *
 * @property message The text message to be displayed in the snackbar.
 * @property action An optional [NotificationAction] that the user can perform. This is typically a
 *   single action like "Undo" or "Dismiss". If null, no action button is shown.
 * @property duration The [Duration] for which the snackbar will be visible.
 * @see InAppNotificationVisual
 * @see InAppNotificationStyle.SnackbarNotification
 */
@Stable
data class SnackbarVisual(
    val message: String,
    val action: NotificationAction?,
    val duration: SnackbarDuration,
) : InAppNotificationVisual {
    internal companion object {
        /**
         * Creates a [SnackbarVisual] from an [InAppNotification].
         *
         * This function attempts to convert an [InAppNotification] into a [SnackbarVisual].
         * It expects the notification to have a style of [InAppNotificationStyle.SnackbarNotification].
         *
         * It performs the following checks:
         * - The `contentText` of the notification must not be null.
         * - The notification must have exactly one action.
         *
         * If the notification has a matching style and passes all checks, a [SnackbarVisual] is created.
         * Otherwise, this function returns `null`.
         *
         * @param notification The [InAppNotification] to convert.
         * @return A [SnackbarVisual] if the conversion is successful, `null` otherwise.
         * @throws IllegalStateException if `contentText` is null or if the number of actions is not 1
         * when the style is [InAppNotificationStyle.SnackbarNotification].
         */
        fun from(notification: InAppNotification): SnackbarVisual? =
            notification.toVisual<InAppNotificationStyle.SnackbarNotification, SnackbarVisual> { style ->
                SnackbarVisual(
                    message = checkNotNull(notification.contentText) {
                        "A notification with a SnackbarNotification style must have a contentText not null"
                    },
                    action = checkNotNull(notification.actionsWithoutTap.singleOrNull()) {
                        "A notification with a SnackbarNotification style must have exactly one action"
                    },
                    duration = style.duration,
                )
            }
    }
}

private inline fun <
    reified TStyle : InAppNotificationStyle,
    reified TVisual : InAppNotificationVisual,
    > InAppNotification.toVisual(
    transform: (TStyle) -> TVisual,
): TVisual? {
    return inAppNotificationStyle
        .takeIf { style -> style is TStyle }
        ?.let { style ->
            check(style is TStyle)
            transform(style)
        }
}

private val InAppNotification.actionsWithoutTap: Set<NotificationAction>
    get() = actions
        .filterNot { it is NotificationAction.Tap }
        .toSet()
