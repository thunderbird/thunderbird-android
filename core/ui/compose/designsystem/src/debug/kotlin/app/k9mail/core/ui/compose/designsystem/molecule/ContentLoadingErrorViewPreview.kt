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
                TextTitleMedium(text = "Error")
            },
            loading = {
                TextTitleMedium(text = "Loading...")
            },
            content = {
                TextTitleMedium(text = "Content")
            },
        )
    }
}
