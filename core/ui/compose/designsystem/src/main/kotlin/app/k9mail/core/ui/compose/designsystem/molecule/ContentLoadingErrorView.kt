package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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
    Loading,
    Content,
    Error,
}
