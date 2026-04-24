package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A container view that can animate between a loading view and a content view.
 *
 * @param STATE The type of the state being passed to this view.
 *
 * @param state The state relevant for displaying the content inside this view.
 * @param loading When `state.isLoading` is `true`, this composable function is displayed.
 * @param content When `state.isLoading` is `false`, this composable function is displayed with [state] being passed as
 *   the argument.
 *
 * **IMPORTANT**: This is a delicate API whose usage should be carefully reviewed. It is using [AnimatedContent] and
 * inherits its caveats.
 *
 * The [loading] and [content] composable functions should only use the state being passed to them (if any). If you
 * disregard this advice, make sure to read the documentation of [AnimatedContent] to learn when the composable
 * functions are invoked and what that means for the external state a function fetches.
 */
@Composable
fun <STATE : LoadingState> ContentLoadingView(
    state: STATE,
    loading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable (STATE) -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        AnimatedContent(
            targetState = state,
            label = "ContentLoadingView",
            contentKey = { targetState -> targetState.isLoading },
        ) { targetState ->
            if (targetState.isLoading) {
                loading()
            } else {
                content(targetState)
            }
        }
    }
}

/**
 * Signals [ContentLoadingView] which of its composable function parameters to execute/display.
 */
interface LoadingState {
    val isLoading: Boolean
}

/**
 * Helper that can be use as `state` argument for [ContentLoadingView] when none of the composable function parameters
 * need access to any state.
 */
sealed class ContentLoadingState private constructor(
    override val isLoading: Boolean,
) : LoadingState {
    data object Loading : ContentLoadingState(isLoading = true)
    data object Content : ContentLoadingState(isLoading = false)
}
