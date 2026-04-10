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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SWIPE_BEHAVIOUR_DISMISS_OFFSCREEN_MULTIPLIER = 1.2f
private const val SWIPE_BEHAVIOUR_REVEAL_EXTENSION = 10
private const val ELASTIC_RESISTANCE_START_FRACTION = 0.7f
private const val ELASTIC_RESISTANCE_MIN_FACTOR = 0.1f

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
    initialAnimatedOffset: Float = 0f,
    initialSwipeState: SwipeState = SwipeState.Settled,
    savedLayoutWidth: Float = 0f,
    savedPendingOffset: Float = 0f,
) {
    /**
     * Animatable that tracks and animates the horizontal offset of the swipeable row.
     */
    val animatedOffset by mutableStateOf(
        Animatable(
            initialValue = initialAnimatedOffset,
            typeConverter = Float.VectorConverter,
            label = "SwipeableRowOffset",
        ),
    )

    /**
     * The current direction of the swipe gesture based on the [pendingOffset] value.
     *
     * @return [SwipeDirection.Settled] when no changes, [SwipeDirection.StartToEnd] when offset > 0;
     *  otherwise [SwipeDirection.EndToStart].
     */
    val swipeDirection: SwipeDirection by derivedStateOf {
        when {
            pendingOffset == 0f || pendingOffset.isNaN() -> SwipeDirection.Settled
            pendingOffset > 0f -> SwipeDirection.StartToEnd
            else -> SwipeDirection.EndToStart
        }
    }

    /**
     * The current swipe progress as a fraction from 0 to 1, representing how far the swipe
     * has progressed toward the active behaviour's threshold.
     *
     * @return 0 if the row is at rest, and 1 means the swipe has reached or exceeded
     * the threshold.
     */
    val swipeProgress: Float by derivedStateOf {
        if (layoutWidth == 0f) {
            0f
        } else {
            (animatedOffset.value.absoluteValue / (activeBehaviour.percentageThreshold * layoutWidth)).coerceIn(0f, 1f)
        }
    }

    /**
     * The current state of the swipeable row.
     */
    internal var swipeState: SwipeState by mutableStateOf(initialSwipeState)

    /**
     * Indicates whether swiping from the start edge to the end edge is enabled.
     *
     * @return `true` if the [startToEndBehaviour] is not [SwipeBehaviour.Disabled]; otherwise `false`.
     */
    internal val enableSwipeFromStartToEnd: Boolean
        get() = startToEndBehaviour !is SwipeBehaviour.Disabled

    /**
     * Indicates whether swiping from the end edge to the start edge is enabled.
     *
     * @return `true` if the [endToStartBehaviour] is not [SwipeBehaviour.Disabled; otherwise `false`.
     */
    internal val enableSwipeFromEndToStart: Boolean
        get() = endToStartBehaviour !is SwipeBehaviour.Disabled

    /**
     * Whether the row currently accepts gesture input.
     *
     * @return `true` if gestures can be processed, `false` if the row is dismissed or
     * currently resetting.
     */
    internal val acceptsGestures: Boolean
        get() = swipeState != SwipeState.Resetting && swipeState != SwipeState.Dismissed

    /**
     * Manages accessibility-related state and actions for the swipeable row.
     */
    internal val accessibilityState = AccessibilityState()

    /**
     * The [ExitTransition] to be applied when the swipeable row is being dismissed.
     *
     * @return the [SwipeBehaviour.Dismiss.dismissTransition] if the active behavior is
     * a dismiss action; otherwise, returns [ExitTransition.None].
     */
    internal val activeExitTransition: ExitTransition
        get() = (activeBehaviour as? SwipeBehaviour.Dismiss)?.dismissTransition ?: ExitTransition.None

    private var layoutWidth by mutableFloatStateOf(savedLayoutWidth)
    private var pendingOffset by mutableFloatStateOf(savedPendingOffset)
    private val decayAnimationSpec = splineBasedDecay<Float>(density)

    /**
     * The [SwipeBehaviour] currently controlling calculations and feedback based on [animatedOffset].
     *
     * @return [startToEndBehaviour] if the offset is positive or zero; [endToStartBehaviour] if negative.
     */
    internal val activeBehaviour: SwipeBehaviour
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

    private var dragAnimationJob: Job? = null

    /**
     * [DraggableState] that handles the drag delta during swipe gestures for the swipeable row.
     */
    internal val draggableState = DraggableState { delta ->
        if (swipeState != SwipeState.Swiping && swipeState != SwipeState.Revealed) return@DraggableState

        // When Revealed with non-auto-reset, only allow closing gestures (toward zero)
        if (swipeState == SwipeState.Revealed && activeBehaviour is SwipeBehaviour.Action) {
            if (!isClosingGesture(delta)) {
                return@DraggableState
            }
        }

        val currentOffset = animatedOffset.value
        val targetOffset = currentOffset + delta
        val targetBehaviour = if (targetOffset >= 0f) startToEndBehaviour else endToStartBehaviour
        val targetMaxOffset = when (targetBehaviour) {
            is SwipeBehaviour.Dismiss -> layoutWidth
            is SwipeBehaviour.Reveal, is SwipeBehaviour.Action ->
                (targetBehaviour.percentageThreshold * layoutWidth) + SWIPE_BEHAVIOUR_REVEAL_EXTENSION

            is SwipeBehaviour.Disabled -> 0f
        }

        val isDirectionAllowed = targetOffset == 0f ||
            (targetOffset > 0f && enableSwipeFromStartToEnd) ||
            (targetOffset < 0f && enableSwipeFromEndToStart)

        if (isDirectionAllowed) {
            val resistanceFactor = calculateResistanceFactor(
                currentAbsOffset = currentOffset.absoluteValue,
                maxOffset = targetMaxOffset,
            )
            val dampenedDelta = delta * resistanceFactor
            val newOffset = pendingOffset + dampenedDelta
            pendingOffset = newOffset
            dragAnimationJob?.cancel()
            dragAnimationJob = coroutineScope.launch { animatedOffset.snapTo(pendingOffset) }
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
     * Handles the completion of a drag gesture and determines the final settled state.
     *
     * @param velocity The velocity of the drag gesture when released in pixels per second.
     * @return `true` if the row settled to a revealed or dismissed state, `false` if it
     *  returned to the resting position.
     */
    internal fun onDragStopped(velocity: Float): Boolean {
        dragAnimationJob?.cancel()
        dragAnimationJob = null
        if ((swipeState != SwipeState.Swiping && swipeState != SwipeState.Revealed) || pendingOffset == 0f) {
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
                coroutineScope.launch {
                    animatedOffset.animateTo(
                        targetValue = 0f,
                        activeBehaviour.settleAnimationSpec,
                    )
                }
                swipeState = SwipeState.Settled
                false
            }
        }
    }

    private fun isClosingGesture(delta: Float): Boolean = (animatedOffset.value > 0f && delta < 0f) ||
        (animatedOffset.value < 0f && delta > 0f)

    private fun updateOffset(
        behaviour: SwipeBehaviour,
        finalOffset: Float,
        willSettlePastThreshold: Boolean,
    ) {
        pendingOffset = finalOffset
        coroutineScope.launch {
            animatedOffset.animateTo(targetValue = finalOffset, activeBehaviour.settleAnimationSpec)

            when (behaviour) {
                is SwipeBehaviour.Action if (willSettlePastThreshold) -> {
                    swipeState = SwipeState.Resetting
                    delay(behaviour.autoCloseDelayMillis)
                    pendingOffset = 0f
                    animatedOffset.animateTo(targetValue = 0f, behaviour.settleAnimationSpec)
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
        val finalThreshold = behaviour.percentageThreshold * layoutWidth
        val wouldFlingPastThreshold = decayTarget.absoluteValue >= finalThreshold
        val hasOffsetPassedThreshold = animatedOffset.value.absoluteValue >= finalThreshold
        return hasOffsetPassedThreshold || wouldFlingPastThreshold
    }

    private fun calculateFinalOffset(
        willSettlePastThreshold: Boolean,
        intendedDirection: SwipeDirection,
        behaviour: SwipeBehaviour,
    ): Float {
        val finalOffset = when (behaviour) {
            is SwipeBehaviour.Dismiss if willSettlePastThreshold ->
                layoutWidth * SWIPE_BEHAVIOUR_DISMISS_OFFSCREEN_MULTIPLIER

            is SwipeBehaviour.Reveal if willSettlePastThreshold -> behaviour.percentageThreshold * layoutWidth
            is SwipeBehaviour.Action if willSettlePastThreshold -> behaviour.percentageThreshold * layoutWidth
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

    private fun calculateResistanceFactor(currentAbsOffset: Float, maxOffset: Float): Float {
        if (maxOffset <= 0f) return 0f
        val resistanceStart = maxOffset * ELASTIC_RESISTANCE_START_FRACTION
        return when {
            currentAbsOffset >= maxOffset -> ELASTIC_RESISTANCE_MIN_FACTOR
            currentAbsOffset >= resistanceStart -> {
                val progress = (currentAbsOffset - resistanceStart) / (maxOffset - resistanceStart)
                lerp(start = 1f, stop = ELASTIC_RESISTANCE_MIN_FACTOR, fraction = progress)
            }

            else -> 1f
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

    private val SwipeBehaviour.percentageThreshold: Float
        get() = with(density) { threshold.toPx() } / layoutWidth

    /**
     * Manages accessibility-related state and actions for the swipeable row component.
     */
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

    companion object {
        /**
         * A Saver for [SwipeableRowState] that handles saving and restoring the swipe offset,
         * layout width, and current swipe state across configuration changes.
         */
        fun Saver(
            coroutineScope: CoroutineScope,
            density: Density,
            startToEndBehaviour: SwipeBehaviour,
            endToStartBehaviour: SwipeBehaviour,
            accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction>,
        ): Saver<SwipeableRowState, *> = listSaver(
            save = { state ->
                listOf(
                    state.animatedOffset.value,
                    state.swipeState.name,
                    state.layoutWidth,
                    state.pendingOffset,
                )
            },
            restore = { savedList ->
                SwipeableRowState(
                    coroutineScope = coroutineScope,
                    density = density,
                    startToEndBehaviour = startToEndBehaviour,
                    endToStartBehaviour = endToStartBehaviour,
                    accessibilityActions = accessibilityActions,
                    // Restore saved states
                    initialAnimatedOffset = savedList[0] as Float,
                    initialSwipeState = SwipeState.valueOf(savedList[1] as String),
                    savedLayoutWidth = savedList[2] as Float,
                    savedPendingOffset = savedList[3] as Float,
                )
            },
        )
    }
}

/**
 * Creates and remembers a [SwipeableRowState] that persists across configuration changes.
 *
 * @param startToEndBehaviour The behaviour when swiping from start to end.
 * @param endToStartBehaviour The behaviour when swiping from end to start.
 * @param accessibilityActions Actions for users relying on accessibility services.
 * @return A [SwipeableRowState] to manage swipe state, animations, and interactions.
 */
@Composable
fun rememberSwipeableRowState(
    startToEndBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    endToStartBehaviour: SwipeBehaviour = SwipeBehaviour.Dismiss(),
    accessibilityActions: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
): SwipeableRowState {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val state = rememberSaveable(
        density,
        startToEndBehaviour,
        endToStartBehaviour,
        accessibilityActions,
        saver = SwipeableRowState.Saver(
            coroutineScope = coroutineScope,
            density = density,
            startToEndBehaviour = startToEndBehaviour,
            endToStartBehaviour = endToStartBehaviour,
            accessibilityActions = accessibilityActions,
        ),
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
