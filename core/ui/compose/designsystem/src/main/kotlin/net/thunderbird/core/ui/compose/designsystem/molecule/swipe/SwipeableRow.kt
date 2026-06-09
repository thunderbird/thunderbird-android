package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection.EndToStart
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection.StartToEnd
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.EndToStartAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.StartToEndAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.fork.draggable

/**
 * Provides a swipeable row, allowing users to reveal background actions by swiping in either
 * horizontal direction.
 *
 * @param state The state object that manages the swipe behaviour, created using
 *  [rememberSwipeableRowState]. It holds information about the current swipe progress,
 *  direction, and allows for programmatic control.
 * @param backgroundContent A composable lambda that defines the content to be displayed
 *  in the background when the row is swiped.
 * @param modifier The [Modifier] to be applied to the [SwipeableRow] container.
 * @param gesturesEnabled A [Boolean] to enable or disable the swipe gestures. If `false`,
 *  the row will not be swipeable by touch, but programmatic swipes via the [state] and
 *  accessibility actions will still function. Defaults to `true`.
 * @param onSwipeEnd A callback lambda that is invoked when a swipe gesture is completed
 * @param onSwipeChange Callback invoked when the swipe direction changes during interaction.
 * @param content A composable lambda that defines the main content of the row displayed in
 * the foreground
 */
@Composable
fun SwipeableRow(
    state: SwipeableRowState,
    backgroundContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    gesturesEnabled: Boolean = true,
    onSwipeEnd: (SwipeDirection) -> Unit = {},
    onSwipeChange: (SwipeDirection) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(state, onSwipeChange) {
        snapshotFlow { state.swipeDirection }
            .drop(1) // Skip the initial `Settled` emission
            .distinctUntilChanged()
            .collectLatest { onSwipeChange(it) }
    }

    val accessibilityCustomActions = rememberAccessibilityActions(
        state = state,
        gesturesEnabled = gesturesEnabled,
        onSwipeEnd = { direction ->
            state.accessibilityState.swipeToDirection(direction)
            performHapticFeedback(state, hapticFeedback)
            onSwipeEnd(direction)
        },
    )

    AnimatedContent(
        targetState = state.swipeState == SwipeState.Dismissed,
        transitionSpec = { EnterTransition.None togetherWith state.activeExitTransition },
        label = "SwipeableRowContentAnimation",
        modifier = modifier.testTag(SwipeableRowDefaults.SWIPEABLE_ROW_ANIMATED_CONTENT_TEST_TAG),
    ) { isDismissed ->
        if (!isDismissed) {
            Box(
                modifier = Modifier
                    .testTag(SwipeableRowDefaults.SWIPEABLE_ROW_CORE_ELEMENT_TEST_TAG)
                    .semantics(mergeDescendants = true) { customActions = accessibilityCustomActions }
                    .onSizeChanged { state.onContainerSizeChanged(it) },
                propagateMinConstraints = true,
            ) {
                SwipeableRowBackground(state, gesturesEnabled, backgroundContent)
                Row(
                    modifier = Modifier
                        .testTag(SwipeableRowDefaults.SWIPEABLE_ROW_DRAGGABLE_ELEMENT_TEST_TAG)
                        .draggable(
                            state = state.draggableState,
                            orientation = Orientation.Horizontal,
                            enabled = gesturesEnabled && state.acceptsGestures,
                            onDragStopped = { velocity ->
                                if (state.onDragStopped(velocity)) {
                                    performHapticFeedback(state, hapticFeedback)
                                    onSwipeEnd(state.swipeDirection)
                                }
                            },
                            onDragStarted = { state.onDragStarted() },
                        )
                        .absoluteOffset { IntOffset(x = state.animatedOffset.value.roundToInt(), y = 0) },
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeableRowBackground(
    state: SwipeableRowState,
    gesturesEnabled: Boolean,
    backgroundContent: @Composable RowScope.() -> Unit,
) {
    if (gesturesEnabled && state.swipeDirection != SwipeDirection.Settled) {
        Row(
            content = backgroundContent,
            modifier = Modifier
                .matchParentSize()
                .testTag(SwipeableRowDefaults.SWIPEABLE_ROW_BACKGROUND_CONTENT_TEST_TAG),
        )
    }
}

private fun performHapticFeedback(
    state: SwipeableRowState,
    hapticFeedback: HapticFeedback,
) {
    if (state.activeBehaviour.enableHapticFeedback) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
}

/**
 * Builds accessibility actions for the swipeable row component based on configured custom accessibility
 * actions.
 *
 * @param onSwipeEnd Callback function invoked when an accessibility action is triggered, receiving
 * the [SwipeDirection] corresponding to the triggered action (either [SwipeDirection.StartToEnd]
 * or [SwipeDirection.EndToStart]).
 * @return A list of [CustomAccessibilityAction] to be used by accessibility services through Compose
 * semantics.
 */
@Composable
private fun rememberAccessibilityActions(
    state: SwipeableRowState,
    gesturesEnabled: Boolean,
    onSwipeEnd: (SwipeDirection) -> Unit,
): List<CustomAccessibilityAction> {
    if (!gesturesEnabled) return emptyList()
    val resources = LocalResources.current
    val actions = state.accessibilityActions
    return remember(resources, actions, state.enableSwipeFromStartToEnd, state.enableSwipeFromEndToStart) {
        actions.mapNotNull { action ->
            val direction = when (action) {
                is EndToStartAccessibilityAction if state.enableSwipeFromEndToStart -> EndToStart
                is StartToEndAccessibilityAction if state.enableSwipeFromStartToEnd -> StartToEnd
                else -> null
            }
            direction?.let { direction ->
                val actionName = resources.getString(action.actionStringRes)
                val label = resources.getString(action.descriptionStringRes, actionName)

                CustomAccessibilityAction(label = label) {
                    onSwipeEnd(direction)
                    true
                }
            }
        }
    }
}

object SwipeableRowDefaults {
    const val SWIPEABLE_ROW_ANIMATED_CONTENT_TEST_TAG = "SwipeableRow_AnimatedContent"
    const val SWIPEABLE_ROW_CORE_ELEMENT_TEST_TAG = "SwipeableRow_core_element"
    const val SWIPEABLE_ROW_BACKGROUND_CONTENT_TEST_TAG = "SwipeableRow_background_content"
    const val SWIPEABLE_ROW_DRAGGABLE_ELEMENT_TEST_TAG = "SwipeableRow_draggable_element"
}
