package app.k9mail.feature.account.server.settings.ui.outgoing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.AccountTopAppBar
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract.ViewModel

@Composable
fun OutgoingServerSettingsScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            Effect.NavigateNext -> onNext()
        }
    }

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.LoadAccountState)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            if (viewModel.mode == InteractionMode.Edit) {
                TopAppBarWithBackButton(
                    title = stringResource(id = R.string.account_server_settings_outgoing_top_bar_title),
                    onBackClick = { dispatch(Event.OnBackClicked) },
                )
            } else {
                AccountTopAppBar(
                    title = stringResource(id = R.string.account_server_settings_outgoing_top_bar_title),
                )
            }
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                modifier = Modifier.imePadding(),
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        OutgoingServerSettingsContent(
            mode = viewModel.mode,
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}
