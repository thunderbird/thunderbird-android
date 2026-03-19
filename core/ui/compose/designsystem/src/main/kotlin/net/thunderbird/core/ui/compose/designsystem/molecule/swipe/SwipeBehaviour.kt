package net.thunderbird.core.ui.compose.designsystem.molecule.swipe

import androidx.annotation.FloatRange
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Immutable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeBehaviour.Companion.DEFAULT_AUTO_RESET_DELAY_MILLIS
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeBehaviour.Companion.DEFAULT_THRESHOLD

/**
 * Defines the behaviour of a swipeable component when a swipe gesture is
 * performed.
 */
@Immutable
sealed interface SwipeBehaviour {
    /**
     * The threshold value that determines when a swipe gesture should trigger
     * a complete swipe action.
     *
     * This value represents the proportion of the swipeable area that must
     * be traversed before the swipe is considered complete. When the swipe
     * distance exceeds this threshold, the row will animate to its final
     * swiped position.
     *
     * If the threshold is not met when the gesture ends, the row will animate
     * back to its original position.
     *
     * The value must be between 0.25 and 1.0, where:
     * - 0.25 means the swipe is triggered when 25% of the available swipe
     *   distance is traversed
     * - 1.0 means the entire swipe distance must be traversed to trigger
     *   the action
     *
     * A lower threshold makes it easier to trigger swipe actions, while a higher
     * threshold requires more deliberate swipe gestures.
     */
    @get:FloatRange(from = 0.25, to = 1.0)
    val threshold: Float

    /**
     * The animation specification used to animate the swipe transition between states.
     *
     * This defines how the row animates when transitioning during swipe gestures, including
     * the duration, easing curve, and other animation parameters. The animation is applied
     * to the horizontal offset of the swipeable content as it moves between its settled
     * position and the fully swiped state.
     */
    val animationSpec: AnimationSpec<Float>

    /**
     * Determines whether haptic feedback should be triggered during swipe interactions.
     *
     * When set to `true`, the device will provide tactile feedback to the user during
     * swipe gestures, enhancing the user experience with physical confirmation of actions.
     * When set to `false`, no haptic feedback will be generated.
     */
    val enableHapticFeedback: Boolean

    /**
     * Defines a reveal swipe behaviour that allows content to be shown or hidden
     * through swipe gestures.
     *
     * @property threshold The fraction of the swipeable area that must be traversed
     *  before the row settles into the revealed state. Must be between 0.25 and 1.0.
     *  Defaults to [DEFAULT_THRESHOLD]
     * @property autoReset Whether the row should automatically return to its initial
     *  position after being revealed after a while. Defaults to `false`
     * @property autoResetDelayMillis The duration to wait before automatically resetting
     *  the revealed state back to the initial position. Only applies when [autoReset]
     *  is enabled. Defaults to [DEFAULT_AUTO_RESET_DELAY_MILLIS].
     */
    data class Reveal(
        @get:FloatRange(from = 0.25, to = 1.0)
        override val threshold: Float = DEFAULT_THRESHOLD,
        override val animationSpec: AnimationSpec<Float> = DefaultAnimation,
        override val enableHapticFeedback: Boolean = true,
        val autoReset: Boolean = false,
        val autoResetDelayMillis: Duration = DEFAULT_AUTO_RESET_DELAY_MILLIS.milliseconds,
    ) : SwipeBehaviour

    /**
     * Defines a reveal swipe behaviour that allows content to be dismissed by swiping
     * past the threshold.
     *
     * This behaviour differs from Reveal in that it is typically used for destructive
     * or final actions where the row should be removed from view after completing the
     * swipe gesture.
     *
     * @property threshold The fraction of the swipeable area that must be traversed
     *  before the row settles into the revealed state. Must be between 0.25 and 1.0.
     *  Defaults to [DEFAULT_THRESHOLD]
     */
    data class Dismiss(
        @get:FloatRange(from = 0.25, to = 1.0)
        override val threshold: Float = DEFAULT_THRESHOLD,
        override val animationSpec: AnimationSpec<Float> = DefaultAnimation,
        override val enableHapticFeedback: Boolean = true,
        val dismissTransition: ExitTransition = fadeOut(),
    ) : SwipeBehaviour

    /**
     * A [SwipeBehaviour] that completely disables swipe functionality.
     *
     * Use this when you want to temporarily or permanently disable swiping on a
     * [SwipeableRow] while keeping the component structure intact.
     */
    data object Disabled : SwipeBehaviour {
        override val threshold: Float = 1f
        override val animationSpec: AnimationSpec<Float> = DefaultAnimation
        override val enableHapticFeedback: Boolean = false
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.5f
        const val DEFAULT_AUTO_RESET_DELAY_MILLIS = 500L

        val DefaultAnimation = SpringSpec(visibilityThreshold = 0.01f)
    }
}
