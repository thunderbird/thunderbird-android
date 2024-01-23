package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.ViewModel
import app.k9mail.feature.account.setup.ui.specialfolders.fake.FakeSpecialFoldersViewModel

@Composable
fun SpecialFoldersScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateNext -> onNext()
            Effect.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadSpecialFolderOptions)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    isNextEnabled = state.value.isLoading.not(),
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        SpecialFoldersContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@PreviewDevices
internal fun SpecialFoldersScreenK9Preview() {
    K9Theme {
        SpecialFoldersScreen(
            onNext = {},
            onBack = {},
            viewModel = FakeSpecialFoldersViewModel(),
        )
    }
}
