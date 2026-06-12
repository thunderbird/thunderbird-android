package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium

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
