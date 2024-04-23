package app.k9mail.feature.account.edit.ui.server.settings.save

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Effect
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.Event
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsContract.ViewModel

@Composable
fun SaveServerSettingsScreen(
    title: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext()
            Effect.NavigateBack -> onBack()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.SaveServerSettings)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = title,
                onBackClick = {
                    dispatch(Event.OnBackClicked)
                },
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = {
                    dispatch(Event.OnBackClicked)
                },
                state = WizardNavigationBarState(
                    showNext = false,
                    isBackEnabled = state.value.error != null,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        SaveServerSettingsContent(
            state = state.value,
            contentPadding = innerPadding,
        )
    }
}
