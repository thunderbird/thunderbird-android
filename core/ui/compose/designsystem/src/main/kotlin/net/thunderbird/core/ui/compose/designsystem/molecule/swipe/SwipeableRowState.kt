package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWIPE_BEHAVIOUR_DISMISS_OFFSCREEN_MULTIPLIER = 1.2f
private const val SWIPE_BEHAVIOUR_REVEAL_EXTENSION = 10

/**
 * Manages the state of a swipeable row component, handling swipe gestures,
 * animations, and drag interactions.
 *
 * @param coroutineScope The coroutine scope used for launching animations and
 *  delayed operations
 * @param density The density of the screen, used for calculating decay animations
 * @param startToEndBehaviour The swipe behaviour for the [start-to-end][SwipeDirection.StartToEnd]
 *  direction. Use [SwipeBehaviour.Disabled] to prevent swiping in this direction.
 * @param endToStartBehaviour The swipe behaviour for the [end-to-start][SwipeDirection.EndToStart]
 *  direction. Use [SwipeBehaviour.Disabled] to prevent swiping in this direction.
 * @param accessibilityActions The list of accessibility actions exposed as custom
 *  semantics actions, allowing TalkBack users to trigger swipe actions programmatically
 */
@Stable
class SwipeableRowState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val density: Density,
    private val startToEndBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    private val endToStartBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    internal val accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
) {
    /**
     * Animatable that tracks and animates the horizontal offset of the swipeable row.
     */
    val animatedOffset by mutableStateOf(
        Animatable(
            initialValue = 0f,
            typeConverter = Float.VectorConverter,
            label = "SwipeableRowOffset",
        ),
    )

    /**
     * The current direction of the swipe gesture based on the animated offset value.
     *
     * This property is derived from the animated offset and determines the swipe state:
     * - [SwipeDirection.Settled] when the offset is zero or NaN, indicating no active swipe
     * - [SwipeDirection.StartToEnd] when the offset is positive, indicating a rightward swipe
     * - [SwipeDirection.EndToStart] when the offset is negative, indicating a leftward swipe
     *
     * The value updates automatically as the swipe gesture progresses and is used to
     * coordinate the display of directional background content and trigger callbacks.
     */
    val swipeDirection: SwipeDirection by derivedStateOf {
        when {
            animatedOffset.value == 0f || animatedOffset.value.isNaN() -> SwipeDirection.Settled
            animatedOffset.value > 0f -> SwipeDirection.StartToEnd
            else -> SwipeDirection.EndToStart
        }
    }

    internal var swipeState: SwipeState by mutableStateOf(SwipeState.Settled)

    /**
     * Indicates whether swiping from the start edge to the end edge is enabled.
     *
     * @returns `true` if the [startToEndBehaviour] is not [SwipeBehaviour.Disabled],
     * allowing the user to perform a swipe gesture from the start edge toward the end
     * edge of the row. Returns `false` if the behaviour is disabled, preventing any
     * start-to-end swipe gestures.
     */
    internal val enableSwipeFromStartToEnd: Boolean
        get() = startToEndBehaviour !is SwipeBehaviour.Disabled

    /**
     * Indicates whether swiping from the end edge to the start edge is enabled.
     *
     * @returns `true` if the [endToStartBehaviour] is not [SwipeBehaviour.Disabled],
     * allowing the user to perform a swipe gesture from the end edge toward the start
     * edge of the row. Returns `false` if the behaviour is disabled, preventing any
     * end-to-start swipe gestures.
     */
    internal val enableSwipeFromEndToStart: Boolean
        get() = swipeState != SwipeState.Resetting && swipeState != SwipeState.Dismissed

    internal val acceptsGestures: Boolean
        get() = swipeState != SwipeState.Resetting && swipeState != SwipeState.Dismissed

    internal val accessibilityState = AccessibilityState()

    internal val dismissTransition: ExitTransition
        get() = (activeBehaviour as? SwipeBehaviour.Dismiss)?.dismissTransition ?: ExitTransition.None

    private var layoutWidth by mutableFloatStateOf(0f)
    private var pendingOffset by mutableFloatStateOf(0f)
    private val decayAnimationSpec = splineBasedDecay<Float>(density)

    private val activeBehaviour: SwipeBehaviour
        get() = when {
            animatedOffset.value > 0f -> startToEndBehaviour
            animatedOffset.value < 0f -> endToStartBehaviour
            else -> startToEndBehaviour
        }

    private val SwipeDirection.behaviour: SwipeBehaviour
        get() = when (this) {
            SwipeDirection.StartToEnd -> startToEndBehaviour
            SwipeDirection.EndToStart -> endToStartBehaviour
            SwipeDirection.Settled -> startToEndBehaviour
        }

    /**
     * [DraggableState] that handles the drag delta during swipe gestures for the swipeable row.
     *
     * This state processes each drag delta and updates the swipe offset based on various constraints
     * and conditions. It ensures that the swipe gesture respects the configured maximum allowed offset,
     * directional permissions, and special behaviour for revealed states.
     */
    internal val draggableState = DraggableState { delta ->
        if (swipeState != SwipeState.Swiping && swipeState != SwipeState.Revealed) return@DraggableState

        val targetOffset = animatedOffset.value + delta
        val targetBehaviour = if (targetOffset >= 0f) startToEndBehaviour else endToStartBehaviour
        val targetMaxOffset = when (targetBehaviour) {
            is SwipeBehaviour.Dismiss -> layoutWidth
            is SwipeBehaviour.Reveal -> (targetBehaviour.threshold * layoutWidth) + SWIPE_BEHAVIOUR_REVEAL_EXTENSION
            is SwipeBehaviour.Disabled -> 0f
        }
        if (targetOffset.absoluteValue < targetMaxOffset) {
            val isDirectionAllowed = targetOffset == 0f ||
                (targetOffset > 0f && enableSwipeFromStartToEnd) ||
                (targetOffset < 0f && enableSwipeFromEndToStart)

            if (isDirectionAllowed) {
                val newOffset = pendingOffset + delta
                pendingOffset = newOffset
                coroutineScope.launch {
                    animatedOffset.animateTo(
                        targetValue = pendingOffset,
                        animationSpec = activeBehaviour.animationSpec,
                    )
                }
            }
        }
    }

    /**
     * Updates the container width when the size of the swipeable row changes.
     *
     * This method should be called when the composable container's size changes to ensure proper
     * swipe behaviour and offset calculations based on the current layout dimensions.
     *
     * @param size The new size of the container in pixels
     */
    internal fun onContainerSizeChanged(size: IntSize) {
        layoutWidth = size.width.toFloat()
    }

    /**
     * Called when a drag gesture starts on the swipeable row.
     */
    internal fun onDragStarted() {
        if (swipeState == SwipeState.Settled) {
            swipeState = SwipeState.Swiping
        }
    }

    /**
     * Handles the completion of a drag gesture on the swipeable row and determines
     * the final settled state.
     *
     * This method is called when the user releases their touch during a swipe gesture.
     *
     * @param velocity The velocity of the drag gesture when released, measured in
     *  pixels per second.
     *  Positive values indicate movement from start to end, negative values indicate
     *  movement from end to start
     * @return `true` if the swipe has completed and the row settled to a non-zero
     *  offset position (revealed or dismissed), `false` if the row returned to its
     *  resting position at zero offset
     */
    internal fun onDragStopped(velocity: Float): Boolean {
        if (swipeState != SwipeState.Swiping && swipeState != SwipeState.Revealed || pendingOffset == 0f) {
            return false
        }

        val intendedDirection = resolveIntendedDirection(velocity)
        return when {
            swipeState == SwipeState.Revealed && !isClosingGesture(velocity) -> false
            isFlingDirectionAllowed(intendedDirection) -> {
                val behaviour = intendedDirection.behaviour
                val willSettlePastThreshold = willSettlePastThreshold(velocity, behaviour)
                val finalOffset = calculateFinalOffset(willSettlePastThreshold, intendedDirection, behaviour)

                updateOffset(behaviour, finalOffset, willSettlePastThreshold)
                willSettlePastThreshold
            }

            else -> {
                pendingOffset = 0f
                coroutineScope.launch { animatedOffset.animateTo(targetValue = 0f, activeBehaviour.animationSpec) }
                swipeState = SwipeState.Settled
                false
            }
        }
    }

    private fun isClosingGesture(delta: Float): Boolean = (animatedOffset.value > 0f && delta < 0f) ||
        (animatedOffset.value < 0f && delta > 0f)

    internal fun updateOffset(
        behaviour: SwipeBehaviour,
        finalOffset: Float,
        willSettlePastThreshold: Boolean,
    ) {
        pendingOffset = finalOffset
        coroutineScope.launch {
            animatedOffset.animateTo(targetValue = finalOffset, activeBehaviour.animationSpec)

            when (behaviour) {
                is SwipeBehaviour.Reveal if (behaviour.autoReset && willSettlePastThreshold) -> {
                    swipeState = SwipeState.Resetting
                    delay(behaviour.autoResetDelayMillis)
                    pendingOffset = 0f
                    animatedOffset.animateTo(targetValue = 0f, behaviour.animationSpec)
                    swipeState = SwipeState.Settled
                }

                is SwipeBehaviour.Reveal if willSettlePastThreshold -> swipeState = SwipeState.Revealed
                is SwipeBehaviour.Dismiss if willSettlePastThreshold -> swipeState = SwipeState.Dismissed
                else -> Unit
            }
        }
    }

    private fun willSettlePastThreshold(velocity: Float, behaviour: SwipeBehaviour): Boolean {
        val decayTarget = decayAnimationSpec.calculateTargetValue(animatedOffset.value, velocity)
        val finalThreshold = behaviour.threshold * layoutWidth
        val wouldFlingPastThreshold = decayTarget.absoluteValue >= finalThreshold
        val hasOffsetPassedThreshold = animatedOffset.value.absoluteValue >= finalThreshold
        return hasOffsetPassedThreshold || wouldFlingPastThreshold
    }

    internal fun calculateFinalOffset(
        willSettlePastThreshold: Boolean,
        intendedDirection: SwipeDirection,
        behaviour: SwipeBehaviour,
    ): Float {
        val finalOffset = when (behaviour) {
            is SwipeBehaviour.Dismiss if willSettlePastThreshold ->
                layoutWidth * SWIPE_BEHAVIOUR_DISMISS_OFFSCREEN_MULTIPLIER

            is SwipeBehaviour.Reveal if willSettlePastThreshold -> behaviour.threshold * layoutWidth
            is SwipeBehaviour.Disabled -> 0f
            else -> 0f
        }
        val directionMultiplier = when (intendedDirection) {
            SwipeDirection.StartToEnd, SwipeDirection.Settled -> 1
            SwipeDirection.EndToStart -> -1
        }
        val result = finalOffset * directionMultiplier

        return when {
            result > 0f && !enableSwipeFromStartToEnd -> 0f
            result < 0f && !enableSwipeFromEndToStart -> 0f
            else -> result
        }
    }

    private fun resolveIntendedDirection(velocity: Float): SwipeDirection {
        return when {
            animatedOffset.value > 0f -> SwipeDirection.StartToEnd
            animatedOffset.value < 0f -> SwipeDirection.EndToStart
            velocity > 0f -> SwipeDirection.StartToEnd
            velocity < 0f -> SwipeDirection.EndToStart
            else -> SwipeDirection.Settled
        }
    }

    private fun isFlingDirectionAllowed(intendedDirection: SwipeDirection): Boolean {
        if (swipeState == SwipeState.Revealed && activeBehaviour is SwipeBehaviour.Reveal) {
            return intendedDirection.behaviour != activeBehaviour
        }

        return when (intendedDirection) {
            SwipeDirection.StartToEnd -> enableSwipeFromStartToEnd
            SwipeDirection.EndToStart -> enableSwipeFromEndToStart
            SwipeDirection.Settled -> true
        }
    }

    internal inner class AccessibilityState {
        internal fun swipeToDirection(direction: SwipeDirection) {
            val finalOffset = calculateFinalOffset(
                willSettlePastThreshold = true,
                intendedDirection = direction,
                behaviour = direction.behaviour,
            )
            updateOffset(
                behaviour = direction.behaviour,
                finalOffset = finalOffset,
                willSettlePastThreshold = true,
            )
        }
    }
}

/**
 * Creates and remembers a SwipeableRowState that controls the swipe behaviour of a swipeable row.
 *
 * This composable function creates a state object that manages the swipe gesture handling,
 * animations, and accessibility actions for a swipeable row component. The state is remembered
 * across recompositions and will be recreated when any of the specified parameters change.
 *
 * @param startToEndBehaviour The swipe behaviour configuration that determines how the row responds to swipe
 * gestures. Can be either Reveal (shows actions and optionally auto-resets) or Dismiss (removes
 * the row). Defaults to Dismiss with default threshold.
 * @param accessibilityActions An immutable list of accessibility actions that define custom
 * swipe actions for accessibility services. These actions allow users with accessibility needs
 * to trigger swipe gestures programmatically. Defaults to an empty list.
 * @return A remembered SwipeableRowState instance that manages the swipe state and behaviour.
 */
@Composable
fun rememberSwipeableRowState(
    startToEndBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    endToStartBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
): SwipeableRowState {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val state = remember(
        density,
        startToEndBehaviour,
        endToStartBehaviour,
        accessibilityActions,
    ) {
        SwipeableRowState(
            coroutineScope = coroutineScope,
            density = density,
            startToEndBehaviour = startToEndBehaviour,
            endToStartBehaviour = endToStartBehaviour,
            accessibilityActions = accessibilityActions,
        )
    }
    return state
}

internal enum class SwipeState { Dismissed, Swiping, Settled, Revealed, Resetting }
