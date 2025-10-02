package net.thunderbird.feature.notification.api.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntSize

private const val A_QUARTER = 4

/**
 * Defines a custom animation for a banner that slides in and out vertically.
 *
 * The banner fades in and slides in from the top when appearing,
 * and fades out and slides out towards the top when disappearing.
 *
 * The size transformation ensures that the width of the banner changes immediately to the target width,
 * while the height animates from the initial height to the target height.
 * The height animation is structured in keyframes:
 * - For the first quarter of the animation duration, the height remains the initial height.
 * - After the first quarter, the height transitions to the target height.
 *
 * @param T The type of the content being animated.
 * @return A [ContentTransform] object that specifies the enter and exit transitions,
 *         as well as the size transformation.
 */
fun <T> AnimatedContentTransitionScope<T>.bannerSlideInSlideOutAnimationSpec(): ContentTransform {
    val enter = fadeIn() + expandVertically()
    val exit = fadeOut() + shrinkVertically()
    return (enter togetherWith exit) using SizeTransform { initialSize, targetSize ->
        this.contentAlignment
        if (targetState != null) {
            keyframes {
                IntSize(width = targetSize.width, height = initialSize.height) at durationMillis / A_QUARTER
                IntSize(width = targetSize.width, height = targetSize.height)
            }
        } else {
            keyframes {
                IntSize(width = initialSize.width, height = initialSize.height) at durationMillis / A_QUARTER
                IntSize(width = initialSize.width, height = 0)
            }
        }
    }
}
