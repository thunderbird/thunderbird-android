package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.annotation.StringRes
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import app.k9mail.core.ui.compose.designsystem.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.EndToStartAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.StartToEndAccessibilityAction

/**
 * State for [SwipeableRow] that manages swipe direction tracking, early completion thresholds,
 * accessibility actions, and dismiss state.
 *
 * Use [rememberSwipeableRowState] to create and remember an instance of this state.
 *
 * @param dismissBoxState The underlying Material3 [SwipeToDismissBoxState].
 * @param swipeActionThreshold Function that returns an optional threshold value (from 0.0 to 1.0)
 *  for early swipe completion based on the given [SwipeDirection]. Returns `null` if no early
 *  completion is desired for that direction.
 * @param customAccessibilityActions A list of [SwipeDirectionAccessibilityAction] to expose
 *  custom accessibility actions for the swipe directions. This allows users of accessibility
 *  services to trigger swipe actions programmatically.
 */
@Stable
class SwipeableRowState internal constructor(
    internal val dismissBoxState: SwipeToDismissBoxState,
    val swipeActionThreshold: (SwipeDirection) -> Float?,
    private val customAccessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
) {
    internal var lastDirection by mutableStateOf<SwipeDirection?>(value = null)
    internal var hasDismissed by mutableStateOf(value = false)

    /**
     * The fraction of the progress going from currentValue to targetValue, within [0f..1f] bounds.
     */
    val progress: Float get() = dismissBoxState.progress

    /**
     * The direction in which the composable is being dismissed, or [SwipeDirection.Settled]
     * if not being dismissed.
     */
    val dismissDirection: SwipeDirection get() = dismissBoxState.dismissDirection.toDirection()

    /**
     * The current settled value of the swipe state.
     */
    val currentValue: SwipeDirection get() = dismissBoxState.currentValue.toDirection()

    /**
     * Builds accessibility actions for the swipeable row component based on configured custom accessibility
     * actions.
     *
     * This composable function creates a list of [CustomAccessibilityAction] instances that allow users with
     * accessibility services to programmatically trigger swipe gestures without performing physical swipe
     * gestures. Each action is mapped from the configured custom accessibility actions with localized
     * descriptions.
     *
     * @param onSwipeEnd Callback function invoked when an accessibility action is triggered, receiving
     * the [SwipeDirection] corresponding to the triggered action (either [SwipeDirection.StartToEnd]
     * or [SwipeDirection.EndToStart]).
     * @return A list of [CustomAccessibilityAction] to be used by accessibility services through Compose
     * semantics.
     */
    @Composable
    internal fun buildAccessibilityActions(
        enableDismissFromStartToEnd: Boolean,
        enableDismissFromEndToStart: Boolean,
        gesturesEnabled: Boolean,
        onSwipeEnd: (SwipeDirection) -> Unit,
    ): List<CustomAccessibilityAction> {
        return customAccessibilityActions
            .mapNotNull { action ->
                val actionText = stringResource(action.actionStringRes)
                val description = stringResource(action.descriptionStringRes, actionText)

                when (action) {
                    is EndToStartAccessibilityAction if enableDismissFromEndToStart && gesturesEnabled ->
                        CustomAccessibilityAction(description) {
                            onSwipeEnd(SwipeDirection.EndToStart)
                            true
                        }

                    is StartToEndAccessibilityAction if enableDismissFromStartToEnd && gesturesEnabled ->
                        CustomAccessibilityAction(description) {
                            onSwipeEnd(SwipeDirection.StartToEnd)
                            true
                        }

                    else -> null
                }
            }
    }

    /**
     * Reset the component to the default position with animation.
     */
    suspend fun reset() {
        dismissBoxState.reset()
    }
}

/**
 * Represents the direction of a swipe gesture in a swipeable layout.
 */
enum class SwipeDirection {
    /** Represents a swipe gesture starting from the left (or start) and moving to the right (or end). **/
    StartToEnd,

    /** Represents a swipe gesture starting from the right (or end) and moving to the left (or start). **/
    EndToStart,

    /** Represents the default or neutral state where no swipe action is in progress. **/
    Settled,
}

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
     * describes the start-to-end swipe behavior to accessibility service users.
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
     * describes the end-to-start swipe behavior to accessibility service users.
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

internal fun SwipeToDismissBoxValue.toDirection(): SwipeDirection =
    when (this) {
        SwipeToDismissBoxValue.StartToEnd -> SwipeDirection.StartToEnd
        SwipeToDismissBoxValue.EndToStart -> SwipeDirection.EndToStart
        SwipeToDismissBoxValue.Settled -> SwipeDirection.Settled
    }

/**
 * Creates and remembers a [SwipeableRowState] for managing swipeable row interactions.
 *
 * This composable function creates a state holder that manages swipe gestures, dismiss animations,
 * and accessibility actions for a swipeable row component. The state is remembered across
 * recompositions and tied to the underlying [SwipeToDismissBoxState].
 *
 * @param swipeActionThreshold Function that determines the early completion threshold for swipe
 * gestures in each direction. Takes a [SwipeDirection] and returns an optional Float value between
 * 0.0 and 1.0 representing the fraction of the swipe distance at which the action should trigger
 * early. Returns null if no early completion is desired for the given direction. Defaults to
 * returning null for all directions.
 * @param accessibilityActions Immutable list of custom accessibility actions that allow users with
 * accessibility services to trigger swipe gestures programmatically. Each action defines localized
 * strings for the action name and description. Defaults to an empty list.
 *
 * @return A remembered [SwipeableRowState] instance that manages swipe direction tracking,
 * dismiss thresholds, and accessibility actions for the swipeable row.
 */
@Composable
fun rememberSwipeableRowState(
    swipeActionThreshold: (SwipeDirection) -> Float? = { null },
    accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
): SwipeableRowState {
    val dismissBoxState = rememberSwipeToDismissBoxState(initialValue = SwipeToDismissBoxValue.Settled)
    return remember(dismissBoxState) {
        SwipeableRowState(dismissBoxState, swipeActionThreshold, accessibilityActions)
    }
}
