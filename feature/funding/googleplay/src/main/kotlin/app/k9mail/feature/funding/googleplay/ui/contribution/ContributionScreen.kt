package app.k9mail.feature.funding.googleplay.ui.contribution

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.funding.googleplay.ui.contribution.ContributionContract.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ContributionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ContributionViewModel>(),
) {
    val (state, dispatch) = viewModel.observe { }

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        ContributionContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}
