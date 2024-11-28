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
internal fun ContentLoadingErrorViewContentPreview() {
    PreviewWithThemes {
        DefaultContentLoadingErrorView(
            state = ContentLoadingErrorState.Content,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingErrorViewLoadingPreview() {
    PreviewWithThemes {
        DefaultContentLoadingErrorView(
            state = ContentLoadingErrorState.Loading,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingErrorViewErrorPreview() {
    PreviewWithThemes {
        DefaultContentLoadingErrorView(
            state = ContentLoadingErrorState.Error,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun ContentLoadingErrorViewInteractivePreview() {
    PreviewWithThemes {
        val state = remember {
            mutableStateOf<ContentLoadingErrorState>(ContentLoadingErrorState.Loading)
        }

        DefaultContentLoadingErrorView(
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
                },
        )
    }
}

@Composable
private fun DefaultContentLoadingErrorView(
    state: ContentLoadingErrorState,
    modifier: Modifier = Modifier,
) {
    ContentLoadingErrorView(
        state = state,
        error = {
            TextTitleMedium(text = "Error")
        },
        loading = {
            TextTitleMedium(text = "Loading...")
        },
        content = {
            TextTitleMedium(text = "Content")
        },
        modifier = modifier.fillMaxSize(),
    )
}
