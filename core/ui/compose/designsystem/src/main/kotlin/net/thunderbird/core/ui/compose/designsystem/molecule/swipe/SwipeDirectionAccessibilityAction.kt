package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import app.k9mail.core.ui.compose.designsystem.R
import net.thunderbird.core.common.resources.StringRes

/**
 * Defines accessibility actions for swipe gestures in horizontal directions.
 */
@Immutable
sealed interface SwipeDirectionAccessibilityAction {
    /**
     * Resolves the localized accessibility action label (e.g., "Delete").
     *
     * This label is used to identify the specific action performed by the swipe gesture
     */
    val actionLabel: @Composable () -> String

    /**
     * String resource ID for the format string used to describe this swipe action.
     *
     * This resource provides a template for accessibility services to explain the result
     */
    @get:StringRes
    val descriptionStringRes: Int

    /**
     * Accessibility action for swipe gestures performed from start to end directions.
     *
     * The description for this action is fixed to a predefined string resource that
     * describes the start-to-end swipe behaviour to accessibility service users.
     *
     * Example:
     * ```xml
     * <string name="designsystem_molecule_swipeable_row_start_to_end_accessibility_description">
     *     Swipe from start to end to %s
     * </string>
     * ```
     * ```kotlin
     * val startToEndAccessibility = StartToEndAccessibilityAction(actionLabel = { "delete this item" })
     * // composed description: "Swipe from start to end to delete this item."
     * ```
     *
     * @property actionLabel Resolver for the label that identifies this swipe action to accessibility services.
     */
    data class StartToEndAccessibilityAction(
        override val actionLabel: @Composable () -> String,
    ) : SwipeDirectionAccessibilityAction {
        override val descriptionStringRes =
            R.string.designsystem_molecule_swipeable_row_start_to_end_accessibility_description
    }

    /**
     * Accessibility action for swipe gestures performed from end to start directions.
     *
     * The description for this action is fixed to a predefined string resource that
     * describes the end-to-start swipe behaviour to accessibility service users.
     *
     * Example:
     * ```xml
     * <string name="designsystem_molecule_swipeable_row_end_to_start_accessibility_description">
     *     Swipe from end to start to %s
     * </string>
     * ```
     * ```kotlin
     * val endToStartAccessibility = EndToStartAccessibilityAction(actionLabel = { "delete this item" })
     * // composed description: "Swipe from end to start to delete this item."
     * ```
     *
     * @property actionLabel Resolver for the label that will be announced by accessibility services when presenting
     * available actions to the user.
     */
    data class EndToStartAccessibilityAction(
        override val actionLabel: @Composable () -> String,
    ) : SwipeDirectionAccessibilityAction {
        override val descriptionStringRes =
            R.string.designsystem_molecule_swipeable_row_end_to_start_accessibility_description
    }
}
