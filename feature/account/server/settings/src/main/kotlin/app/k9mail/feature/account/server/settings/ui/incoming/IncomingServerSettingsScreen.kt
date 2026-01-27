package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Effect
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.Event
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract.ViewModel

@Composable
fun IncomingServerSettingsScreen(
    onNext: (IncomingServerSettingsContract.State) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext(viewModel.state.value)
            is Effect.NavigateBack -> onBack()
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
                    title = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
                    onBackClick = { dispatch(Event.OnBackClicked) },
                )
            } else {
                AccountTopAppBar(
                    title = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
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
        IncomingServerSettingsContent(
            mode = viewModel.mode,
            onEvent = { dispatch(it) },
            state = state.value,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        )
    }
}
