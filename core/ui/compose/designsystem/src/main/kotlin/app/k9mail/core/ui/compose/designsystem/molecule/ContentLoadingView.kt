package app.k9mail.core.ui.compose.designsystem.molecule

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun ContentLoadingView(
    state: ContentLoadingState,
    loading: @Composable () -> Unit,
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
            label = "ContentLoadingView",
        ) { targetState ->
            when (targetState) {
                ContentLoadingState.Loading -> loading()
                ContentLoadingState.Content -> content()
            }
        }
    }
}

enum class ContentLoadingState {
    Loading,
    Content,
}
