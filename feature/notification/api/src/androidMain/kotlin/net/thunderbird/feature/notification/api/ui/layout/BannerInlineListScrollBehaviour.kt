package net.thunderbird.feature.notification.api.ui.layout

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.roundToInt

private const val POST_FLING_UNCONSUMED_THRESHOLD = 0.5f

/**
 * Handles the nested scrolling behavior for the banner inline list that is displayed with a scrollable content.
 *
 * This behavior allows the banner inline list to scroll together with the scrollable content, as if it was part of it.
 * The banner will initially be visible. As the user scrolls down, the banner will scroll off-screen.
 * When the user scrolls up, the banner will reappear when there is no more content to scroll.
 *
 * This class implements [NestedScrollConnection] to intercept scroll events and adjust
 * the banner's visibility and position accordingly.
 *
 * @param state The [BannerInlineListScrollState] that holds the current state of the banner and list.
 * @param flingAnimation The [DecayAnimationSpec] used for animating the banner's collapse/expand during flings.
 */
internal class BannerInlineListScrollBehaviour(
    val state: BannerInlineListScrollState,
    val flingAnimation: DecayAnimationSpec<Float>,
) {
    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val height = state.bannerInlineListHeight
            return when {
                // Return earlier if banner inline height isn't visible.
                height <= 0 -> super.onPreScroll(available, source)

                // We don't consume the scroll if the content was scrolled
                state.contentOffset != 0f -> Offset.Zero

                // We'll consume the scroll offset, to scroll the banner inline if the
                // content isn't scrolled yet, until the banner inline height offset
                // didn't reach its height
                state.contentOffset == 0f -> {
                    val delta = available.y
                    val prevHeightOffset = state.bannerInlineListHeightOffset
                    state.bannerInlineListHeightOffset = (state.bannerInlineListHeightOffset + delta).coerceIn(
                        minimumValue = -height,
                        maximumValue = 0f,
                    )

                    // If the previous and new heightOffset are different, it means the height offset
                    // didn't reach its height, thus we consume the scroll value in this connection.
                    if (prevHeightOffset != state.bannerInlineListHeightOffset) {
                        available.copy(y = delta)
                    } else {
                        Offset.Zero
                    }
                }

                else -> Offset.Zero
            }

            return super.onPreScroll(available, source)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            state.accumulatedContentOffset += consumed.y
            // Return earlier if banner inline height isn't visible.
            if (state.bannerInlineListHeight <= 0f) return super.onPostScroll(consumed, available, source)

            val height = state.bannerInlineListHeight
            val heightOffset = state.bannerInlineListHeightOffset
            val heightOffsetLimit = -height

            if (heightOffset == -height) {
                state.contentOffset += consumed.y

                if (heightOffset == 0f || heightOffset == heightOffsetLimit) {
                    if (consumed.y == 0f && available.y > 0f) {
                        // Reset the total content offset to zero when scrolling all the way down.
                        // This will eliminate some float precision inaccuracies.
                        state.contentOffset = 0f
                    }
                }
            }

            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val heightOffset = state.bannerInlineListHeightOffset
            val height = state.bannerInlineListHeight
            val superConsumed = super.onPostFling(consumed, available)

            if (heightOffset != -height) {
                return Velocity.Zero
            }

            var remaining = available.y
            if (abs(remaining) > 0 && state.contentOffset.roundToInt() == 0 && heightOffset in -height..0f) {
                var lastValue = 0f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = available.y,
                ).animateDecay(flingAnimation) {
                    val delta = value - lastValue
                    val initialHeightOffset = state.bannerInlineListHeightOffset
                    state.bannerInlineListHeightOffset = (initialHeightOffset + delta).coerceAtMost(0f)
                    val consumed = abs(initialHeightOffset - state.bannerInlineListHeightOffset)
                    lastValue = value
                    remaining = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed
                    if (abs(delta - consumed) > POST_FLING_UNCONSUMED_THRESHOLD) {
                        this.cancelAnimation()
                        state.contentOffset = 0f
                    }
                }
            }

            return superConsumed + Velocity(0f, remaining)
        }
    }
}

/**
 * Creates a [BannerInlineListScrollBehaviour] that is remembered across compositions and it's state will survive the
 * activity or process recreation using the saved instance state mechanism.
 *
 * @param state The state object to be used to control or observe the banner inline state.
 * @param flingAnimation The animation spec to be used for flinging content.
 */
@Composable
internal fun rememberBannerInlineScrollBehaviour(
    state: BannerInlineListScrollState = rememberSaveable(saver = BannerInlineListScrollState.Saver) {
        BannerInlineListScrollState()
    },
    flingAnimation: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
): BannerInlineListScrollBehaviour = remember { BannerInlineListScrollBehaviour(state, flingAnimation) }
