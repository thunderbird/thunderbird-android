package app.k9mail.feature.account.setup.ui.specialfolders

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.common.provider.AppNameProvider
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Effect
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.Event
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.ViewModel

@Composable
fun SpecialFoldersScreen(
    onNext: (isManualSetup: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    appNameProvider: AppNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext(effect.isManualSetup)
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
                    showNext = state.value.isManualSetup && state.value.isLoading.not(),
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        SpecialFoldersContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
            appName = appNameProvider.appName,
        )
    }
}
