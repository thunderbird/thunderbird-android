package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.theme.PreviewWithThemes

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ContentLoadingErrorView(
    state: ContentLoadingErrorState,
    loading: @Composable () -> Unit,
    error: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
        contentAlignment = contentAlignment,
    ) {
        AnimatedContent(
            targetState = state,
            label = "ContentLoadingErrorView",
        ) { targetState ->
            when (targetState) {
                ContentLoadingErrorState.Loading -> loading()
                ContentLoadingErrorState.Content -> content()
                ContentLoadingErrorState.Error -> error()
            }
        }
    }
}

enum class ContentLoadingErrorState {
    Loading, Content, Error
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingErrorViewPreview() {
    PreviewWithThemes {
        val state = remember {
            mutableStateOf(ContentLoadingErrorState.Loading)
        }

        ContentLoadingErrorView(
            state = state.value,
            modifier = Modifier
                .clickable {
                    when (state.value) {
                        ContentLoadingErrorState.Loading -> {
                            state.value = ContentLoadingErrorState.Content
                        }

                        ContentLoadingErrorState.Content -> {
                            state.value = ContentLoadingErrorState.Error
                        }

                        ContentLoadingErrorState.Error -> {
                            state.value = ContentLoadingErrorState.Loading
                        }
                    }
                }
                .fillMaxSize(),
            error = {
                TextSubtitle1(text = "Error")
            },
            loading = {
                TextSubtitle1(text = "Loading...")
            },
            content = {
                TextSubtitle1(text = "Content")
            },
        )
    }
}
