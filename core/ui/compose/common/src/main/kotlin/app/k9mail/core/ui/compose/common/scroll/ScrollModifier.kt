package app.k9mail.core.ui.compose.common.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.proxyScroll(
    coroutineScope: CoroutineScope,
    scrollState: ScrollState,
    onCancelFling: () -> Unit,
    onCreateFling: (Job) -> Unit,
): Modifier {
    val velocityTracker = VelocityTracker()
    return pointerInput(Unit) {
        val decay = splineBasedDecay<Float>(this)
        detectVerticalDragGestures(
            onDragStart = {
                velocityTracker.resetTracking()
                onCancelFling()
            },
            onDragEnd = {
                onCancelFling()
                val velocity = -velocityTracker.calculateVelocity().y
                velocityTracker.resetTracking()
                onCreateFling(
                    coroutineScope.launch {
                        var previous = 0f
                        val animatable = Animatable(0f)
                        animatable.animateDecay(
                            initialVelocity = velocity,
                            animationSpec = decay,
                        ) {
                            val delta = value - previous
                            previous = value
                            coroutineScope.launch { scrollState.scrollBy(delta) }
                        }
                    },
                )
            },
            onVerticalDrag = { change, dragAmount ->
                coroutineScope.launch { scrollState.scrollBy(-dragAmount) }
                velocityTracker.addPointerInputChange(change)
                change.consume()
            },
        )
    }
}
