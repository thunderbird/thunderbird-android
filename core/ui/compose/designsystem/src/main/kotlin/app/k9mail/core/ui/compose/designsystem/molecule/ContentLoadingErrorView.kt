package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A container view that can animate between a loading view, an error view, and a content view.
 *
 * @param ERROR The type describing the error.
 * @param STATE The type of the state being passed to this view.
 *
 * @param state The state relevant for displaying the content inside this view.
 * @param loading When `state.isLoading` is `true`, this composable function is displayed.
 * @param error When `state.isLoading` is `false` and `state.error` is not `null`, this composable function is displayed
 *   with `state.error` being passed as the argument.
 * @param content When `state.isLoading` is `false` and `state.error` is `null`, this composable function is displayed
 *   with [state] being passed as the argument.
 *
 * **IMPORTANT**: This is a delicate API whose usage should be carefully reviewed. It is using [AnimatedContent] and
 * inherits its caveats.
 *
 * The [loading], [error] and [content] composable functions should only use the state being passed to them (if any).
 * If you disregard this advice, make sure to read the documentation of [AnimatedContent] to learn when the composable
 * functions are invoked and what that means for the external state a function fetches.
 */
@Composable
fun <ERROR, STATE : LoadingErrorState<ERROR>> ContentLoadingErrorView(
    state: STATE,
    loading: @Composable () -> Unit,
    error: @Composable (ERROR) -> Unit,
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
            label = "ContentLoadingErrorView",
            contentKey = { targetState ->
                ContentKey(isLoading = targetState.isLoading, error = targetState.error)
            },
        ) { targetState ->
            val errorValue = targetState.error
            when {
                targetState.isLoading -> loading()
                errorValue != null -> error(errorValue)
                else -> content(targetState)
            }
        }
    }
}

/**
 * Signals [ContentLoadingErrorView] which of its composable function parameters to execute/display.
 */
interface LoadingErrorState<ERROR> {
    val isLoading: Boolean
    val error: ERROR?
}

private data class ContentKey<ERROR>(
    override val isLoading: Boolean,
    override val error: ERROR?,
) : LoadingErrorState<ERROR>

/**
 * Helper that can be use as `state` argument for [ContentLoadingErrorView] when none of the composable function
 * parameters need access to any state.
 */
sealed class ContentLoadingErrorState private constructor(
    override val isLoading: Boolean,
    override val error: Unit?,
) : LoadingErrorState<Unit> {
    data object Loading : ContentLoadingErrorState(isLoading = true, error = null)
    data object Content : ContentLoadingErrorState(isLoading = false, error = null)
    data object Error : ContentLoadingErrorState(isLoading = false, error = Unit)
}
