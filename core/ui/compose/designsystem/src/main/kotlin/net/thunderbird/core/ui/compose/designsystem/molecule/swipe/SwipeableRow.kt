package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection.EndToStart
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection.StartToEnd

/**
 * A composable that provides a swipeable row, allowing users to reveal background actions
 * by swiping in either horizontal direction.
 *
 * This component wraps its main [content] in a container that can be swiped from
 * [Start-to-End][StartToEnd] (e.g., left-to-right in LTR layouts) or [End-to-Start][EndToStart].
 * Swiping reveals the [backgroundContent], which can be customized based on the swipe direction.
 *
 * The swipe behavior can be configured using the [state], including setting thresholds
 * that trigger actions automatically when a certain swipe distance is reached.
 *
 * It also supports accessibility by exposing custom actions that correspond to the
 * available swipe gestures.
 *
 * @param state The state object that manages the swipe behavior, created using
 *  [rememberSwipeableRowState]. It holds information about the current swipe progress,
 *  direction, and allows for programmatic control.
 * @param backgroundContent A composable lambda that defines the content to be displayed
 *  in the background when the row is swiped. It receives the current [SwipeDirection]
 *  ([StartToEnd] or [EndToStart]) to allow for different UIs based on the swipe direction.
 * @param modifier The [Modifier] to be applied to the [SwipeableRow] container.
 * @param enableDismissFromStartToEnd A [Boolean] indicating whether swiping from start to end
 *  (e.g., left to right in LTR) is enabled. Defaults to `true`.
 * @param enableDismissFromEndToStart A [Boolean] indicating whether swiping from end to start
 *  (e.g., right to left in LTR) is enabled. Defaults to `true`.
 * @param gesturesEnabled A [Boolean] to enable or disable the swipe gestures. If `false`,
 *  the row will not be swipeable by touch, but programmatic swipes via the [state] and
 *  accessibility actions will still function. Defaults to `true`.
 * @param onSwipeEnd A callback lambda that is invoked when a swipe gesture is completed
 */
@Composable
fun SwipeableRow(
    state: SwipeableRowState,
    backgroundContent: @Composable RowScope.(direction: SwipeDirection) -> Unit,
    modifier: Modifier = Modifier,
    enableDismissFromStartToEnd: Boolean = true,
    enableDismissFromEndToStart: Boolean = true,
    gesturesEnabled: Boolean = true,
    onSwipeEnd: (SwipeDirection) -> Unit = {},
    onSwipeChange: (SwipeDirection) -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val currentOnSwipeEnd by rememberUpdatedState(onSwipeEnd)
    val currentOnSwipeChange by rememberUpdatedState(onSwipeChange)
    val currentSwipeActionThreshold by rememberUpdatedState(state.swipeActionThreshold)
    val accessibilityActions = state.buildAccessibilityActions(
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        gesturesEnabled = gesturesEnabled,
        onSwipeEnd = currentOnSwipeEnd,
    )
    LaunchedEffect(state.lastDirection) {
        state.lastDirection?.let(currentOnSwipeChange)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.progress }
            .collect { progress ->
                val direction = state.lastDirection
                if (direction != null) {
                    val threshold = currentSwipeActionThreshold(direction) ?: Float.MAX_VALUE
                    if (1f - progress >= threshold) {
                        state.reset()
                        currentOnSwipeEnd(direction)
                        delay(500.milliseconds)
                    }
                }
            }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.dismissBoxState.currentValue }
            .filter { it == SwipeToDismissBoxValue.Settled }
            .collect { state.hasDismissed = false }
    }
    SwipeToDismissBox(
        state = state.dismissBoxState,
        enableDismissFromStartToEnd = enableDismissFromStartToEnd,
        enableDismissFromEndToStart = enableDismissFromEndToStart,
        gesturesEnabled = gesturesEnabled,
        backgroundContent = { SwipeableRowBackgroundContent(state, gesturesEnabled, backgroundContent) },
        modifier = modifier.semantics { customActions = accessibilityActions },
        onDismiss = {
            if (!state.hasDismissed) {
                state.hasDismissed = true
                if (currentSwipeActionThreshold(it.toDirection()) != null) {
                    coroutineScope.launch { state.reset() }
                }
                onSwipeEnd(it.toDirection())
            }
        },
        content = content,
    )
}

@Composable
private fun RowScope.SwipeableRowBackgroundContent(
    state: SwipeableRowState,
    gesturesEnabled: Boolean,
    backgroundContent: @Composable RowScope.(direction: SwipeDirection) -> Unit,
) {
    if (gesturesEnabled) {
        val direction = state.dismissDirection
        SideEffect {
            if (state.lastDirection != direction) {
                state.lastDirection = direction
            }
        }
        backgroundContent(direction)
    }
}
