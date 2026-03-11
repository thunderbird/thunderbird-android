package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
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
import app.k9mail.core.ui.compose.designsystem.R
import kotlin.math.absoluteValue
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.core.common.resources.StringRes

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
    val animatedOffset = Animatable(
        initialValue = 0f,
        typeConverter = Float.VectorConverter,
        label = "SwipeableRowOffset",
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
        get() = endToStartBehaviour !is SwipeBehaviour.Disabled

    private var layoutWidth by mutableFloatStateOf(0f)
    private val offset = MutableStateFlow(0f)
    private var hasDragStopped by mutableStateOf(false)
    private var isRevealed by mutableStateOf(false)
    private val decayAnimationSpec = splineBasedDecay<Float>(density)

    private val activeBehaviour: SwipeBehaviour
        get() = when {
            animatedOffset.value > 0f -> startToEndBehaviour
            animatedOffset.value < 0f -> endToStartBehaviour
            else -> startToEndBehaviour
        }

    /**
     * [DraggableState] that handles the drag delta during swipe gestures for the swipeable row.
     *
     * This state processes each drag delta and updates the swipe offset based on various constraints
     * and conditions. It ensures that the swipe gesture respects the configured maximum allowed offset,
     * directional permissions, and special behaviour for revealed states.
     */
    internal val draggableState = DraggableState { delta ->
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

            val isMovingTowardZero = (animatedOffset.value > 0f && delta < 0f) ||
                (animatedOffset.value < 0f && delta > 0f)

            val isRevealRestore = activeBehaviour is SwipeBehaviour.Reveal &&
                isRevealed &&
                isMovingTowardZero

            if (!hasDragStopped && (isDirectionAllowed || isRevealRestore)) {
                offset.update { currentOffset ->
                    val newOffset = currentOffset + delta
                    if (isRevealRestore) clampTowardZero(newOffset) else newOffset
                }
            }
        }
    }

    /**
     * Collects offset changes and drives the swipe animation.
     *
     * Must be called from a coroutine whose lifecycle is tied to the composition
     * (e.g., via [LaunchedEffect]) so that collection is cancelled when the state
     * leaves the tree or is recreated.
     */
    internal fun observeOffset(): Job {
        return offset.onEach { offset ->
            if (hasDragStopped) {
                animatedOffset.animateTo(targetValue = offset, animationSpec = activeBehaviour.animationSpec)
            } else {
                animatedOffset.snapTo(targetValue = offset)
            }
        }.launchIn(coroutineScope)
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
        hasDragStopped = false
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
        hasDragStopped = true

        val intendedDirection = resolveIntendedDirection(velocity)
        val isFlingAllowed = isFlingDirectionAllowed(intendedDirection)

        if (!isFlingAllowed) {
            offset.update { 0f }
            return false
        }

        val behaviour = intendedDirection.behaviour
        val willSettlePastThreshold = willSettlePastThreshold(velocity, behaviour)
        val finalOffset = calculateFinalOffset(willSettlePastThreshold, intendedDirection, behaviour)

        isRevealed = behaviour is SwipeBehaviour.Reveal && finalOffset != 0f

        offset.update { finalOffset }

        handlePostResetState(willSettlePastThreshold, behaviour)
        return finalOffset != 0f
    }

    private fun willSettlePastThreshold(velocity: Float, behaviour: SwipeBehaviour): Boolean {
        val decayTarget = decayAnimationSpec.calculateTargetValue(animatedOffset.value, velocity)
        val finalThreshold = behaviour.threshold * layoutWidth
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
            is SwipeBehaviour.Dismiss if willSettlePastThreshold -> layoutWidth
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

    private fun handlePostResetState(willSettlePastThreshold: Boolean, behaviour: SwipeBehaviour) {
        if (behaviour is SwipeBehaviour.Reveal && behaviour.autoReset && willSettlePastThreshold) {
            coroutineScope.launch {
                delay(behaviour.autoResetDelayMillis)
                offset.update { 0f }
            }
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
        val isRevealRestore = activeBehaviour is SwipeBehaviour.Reveal && isRevealed
        return when (intendedDirection) {
            SwipeDirection.StartToEnd -> enableSwipeFromStartToEnd || isRevealRestore
            SwipeDirection.EndToStart -> enableSwipeFromEndToStart || isRevealRestore
            SwipeDirection.Settled -> true
        }
    }

    private val SwipeDirection.behaviour: SwipeBehaviour
        get() = when (this) {
            SwipeDirection.StartToEnd -> startToEndBehaviour
            SwipeDirection.EndToStart -> endToStartBehaviour
            SwipeDirection.Settled -> startToEndBehaviour
        }

    private fun clampTowardZero(offset: Float): Float = if (animatedOffset.value > 0f) {
        offset.coerceAtLeast(0f)
    } else {
        offset.coerceAtMost(0f)
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
    DisposableEffect(state) {
        val job = state.observeOffset()
        onDispose { job.cancel() }
    }
    return state
}
