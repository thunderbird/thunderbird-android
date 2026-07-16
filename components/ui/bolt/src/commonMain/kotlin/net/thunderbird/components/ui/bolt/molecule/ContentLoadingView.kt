package net.thunderbird.components.ui.bolt.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium

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

@Composable
@Preview(showBackground = true)
fun ContentLoadingViewPreview() {
    PreviewWithThemes {
        DefaultContentLoadingView(
            state = ContentLoadingState.Content,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingViewLoadingPreview() {
    PreviewWithThemes {
        DefaultContentLoadingView(
            state = ContentLoadingState.Loading,
        )
    }
}

@Composable
private fun DefaultContentLoadingView(
    state: ContentLoadingState,
    modifier: Modifier = Modifier,
) {
    ContentLoadingView(
        state = state,
        loading = {
            TextTitleMedium(text = "Loading...")
        },
        content = {
            TextTitleMedium(text = "Content")
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingViewInteractivePreview() {
    PreviewWithThemes {
        val state = remember {
            mutableStateOf(State(isLoading = true, content = "Hello world"))
        }

        ContentLoadingView(
            state = state.value,
            loading = {
                TextTitleMedium(text = "Loading...")
            },
            content = { targetState ->
                TextTitleMedium(text = targetState.content)
            },
            modifier = Modifier
                .clickable {
                    val currentValue = state.value
                    state.value = currentValue.copy(isLoading = currentValue.isLoading.not())
                }
                .fillMaxSize(),
        )
    }
}

private data class State(
    override val isLoading: Boolean,
    val content: String,
) : LoadingState
