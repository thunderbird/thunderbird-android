package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Defines the behaviour of a swipeable component when a swipe gesture is
 * performed.
 */
@Immutable
sealed interface SwipeBehaviour {
    /**
     * @return The threshold distance that determines when a swipe gesture should trigger
     */
    val threshold: Dp

    /**
     * @return The animation specification used when the swipe gesture ends and the row
     * settles to its final position (revealed, dismissed, or back to resting).
     */
    val settleAnimationSpec: AnimationSpec<Float>

    /**
     * Determines whether haptic feedback should be triggered during swipe interactions.
     *
     * @return `true`, the device will provide tactile feedback to the user during
     * swipe gestures; otherwise `false`.
     */
    val enableHapticFeedback: Boolean

    /**
     * Defines a reveal swipe behaviour that allows content to be shown or hidden
     * through swipe gestures.
     */
    data class Reveal(
        override val threshold: Dp = DEFAULT_THRESHOLD,
        override val settleAnimationSpec: AnimationSpec<Float> = DefaultSettleAnimation,
        override val enableHapticFeedback: Boolean = true,
    ) : SwipeBehaviour

    /**
     * Defines a swipe behaviour that triggers an action and then automatically
     * resets the swipe state after a specified delay.
     *
     * @property autoCloseDelayMillis The duration after which the swipe state will be reset
     * to its default state.
     */
    data class Action(
        override val threshold: Dp = DEFAULT_THRESHOLD,
        override val settleAnimationSpec: AnimationSpec<Float> = DefaultSettleAnimation,
        override val enableHapticFeedback: Boolean = true,
        val autoCloseDelayMillis: Duration = DEFAULT_AUTO_RESET_DELAY_MILLIS.milliseconds,
    ) : SwipeBehaviour

    /**
     * Defines a reveal swipe behaviour that allows content to be dismissed by swiping
     * past the threshold.
     *
     * @property dismissTransition The exit transition that will be applied when the
     *  item is dismissed.
     */
    data class Dismiss(
        override val threshold: Dp = DEFAULT_THRESHOLD,
        override val settleAnimationSpec: AnimationSpec<Float> = DefaultDismissAnimation,
        override val enableHapticFeedback: Boolean = true,
        val dismissTransition: ExitTransition = fadeOut(tween(durationMillis = 150)) + shrinkVertically(
            tween(
                durationMillis = 200,
                delayMillis = 50,
            ),
        ),
    ) : SwipeBehaviour

    /**
     * A [SwipeBehaviour] that completely disables swipe functionality.
     *
     * Use this when you want to temporarily or permanently disable swiping on a
     * [SwipeableRow] while keeping the component structure intact.
     */
    data object Disabled : SwipeBehaviour {
        override val threshold: Dp = Dp.Unspecified
        override val settleAnimationSpec: AnimationSpec<Float> = DefaultSettleAnimation
        override val enableHapticFeedback: Boolean = false
    }

    companion object {
        internal val DEFAULT_THRESHOLD = 150.dp
        internal const val DEFAULT_AUTO_RESET_DELAY_MILLIS = 500L

        /**
         * The default animation specification used for settling swipe gestures back to
         * their resting position.
         */
        val DefaultSettleAnimation = SpringSpec(
            dampingRatio = 0.75f,
            stiffness = 600f,
            visibilityThreshold = 0.01f,
        )

        /**
         * The default animation specification used for dismissing the [SwipeableRow] when
         * the swipe distance exceeds the [threshold], removing it from screen.
         */
        val DefaultDismissAnimation: AnimationSpec<Float> = tween(
            durationMillis = 250,
            easing = FastOutSlowInEasing,
        )
    }
}
