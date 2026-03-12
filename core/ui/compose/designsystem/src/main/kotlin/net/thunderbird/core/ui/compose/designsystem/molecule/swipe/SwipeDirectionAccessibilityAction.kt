package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.runtime.Immutable
import app.k9mail.core.ui.compose.designsystem.R
import net.thunderbird.core.common.resources.StringRes

/**
 * Defines accessibility actions for swipe gestures in horizontal directions.
 *
 * This sealed interface represents accessibility actions that can be performed on swipeable
 * components, allowing users with accessibility services to trigger swipe gestures
 * programmatically. Each implementation specifies both the action label and description
 * that will be announced to accessibility services.
 *
 * These accessibility actions are integrated into the swipeable row component's semantics
 * to ensure that swipe gestures can be performed through accessibility services without
 * requiring physical touch interactions.
 */
@Immutable
sealed interface SwipeDirectionAccessibilityAction {
    /**
     * String resource ID for the accessibility action label.
     *
     * This resource ID references a localized string that describes the action name
     * displayed to users through accessibility services when performing swipe gestures.
     * The string resource is used to provide a user-friendly label for the custom
     * accessibility action associated with a specific swipe direction.
     *
     * This will compose the full action label for the accessibility action, including
     * the action name and description, so that users can easily identify the action
     * that was triggered.
     */
    @get:StringRes
    val actionStringRes: Int

    /**
     * String resource ID for the description text of this swipe direction accessibility action.
     *
     * This resource is used to provide a detailed description to accessibility services about
     * what happens when this swipe action is performed. The description helps users with
     * accessibility needs to understand the result or effect of executing this action.
     *
     * It will include the [actionStringRes] string resource ID in the description, so that
     * users can easily identify the action that was triggered. For example, the description
     * for the StartToEndAccessibilityAction might be "Swipe from start to end to delete this item."
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
     * <string name="swipe_delete_item">delete this item</string>
     * ```
     * ```kotlin
     * val startToEndAccessibility = StartToEndAccessibilityAction(actionStringRes = R.string.swipe_delete_item)
     * // composed description: "Swipe from start to end to delete this item."
     * ```
     *
     * @property actionStringRes Resource ID for the action label that identifies this
     *  swipe action to accessibility services
     */
    data class StartToEndAccessibilityAction(override val actionStringRes: Int) : SwipeDirectionAccessibilityAction {
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
     * <string name="swipe_delete_item">delete this item</string>
     * ```
     * ```kotlin
     * val endToStartAccessibility = EndToStartAccessibilityAction(actionStringRes = R.string.swipe_delete_item)
     * // composed description: "Swipe from end to start to delete this item."
     * ```
     *
     * @property actionStringRes The string resource ID for the action label that will be announced
     * by accessibility services when presenting available actions to the user.
     */
    data class EndToStartAccessibilityAction(override val actionStringRes: Int) : SwipeDirectionAccessibilityAction {
        override val descriptionStringRes =
            R.string.designsystem_molecule_swipeable_row_end_to_start_accessibility_description
    }
}
