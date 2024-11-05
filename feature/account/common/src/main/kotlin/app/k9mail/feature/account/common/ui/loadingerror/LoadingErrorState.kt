package app.k9mail.feature.account.common.ui.loadingerror

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import app.k9mail.core.ui.compose.designsystem.molecule.ContentLoadingErrorState

interface LoadingErrorState<ERROR> {
    val isLoading: Boolean
    val error: ERROR?
}

@Composable
fun <ERROR> rememberContentLoadingErrorViewState(
    state: LoadingErrorState<ERROR>,
) = remember(key1 = state.isLoading, key2 = state.error) {
    derivedStateOf {
        when {
            state.isLoading -> ContentLoadingErrorState.Loading
            state.error != null -> ContentLoadingErrorState.Error
            else -> ContentLoadingErrorState.Content
        }
    }
}
