package app.k9mail.feature.account.server.settings.ui.incoming

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.common.ui.AccountTopAppBar
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.preview.PreviewAccountStateRepository
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
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        IncomingServerSettingsContent(
            mode = viewModel.mode,
            onEvent = { dispatch(it) },
            state = state.value,
            contentPadding = innerPadding,
        )
    }
}

@Composable
@PreviewDevices
internal fun IncomingServerSettingsScreenK9Preview() {
    K9Theme {
        IncomingServerSettingsScreen(
            onNext = {},
            onBack = {},
            viewModel = IncomingServerSettingsViewModel(
                mode = InteractionMode.Create,
                validator = IncomingServerSettingsValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}

@Composable
@PreviewDevices
internal fun IncomingServerSettingsScreenThunderbirdPreview() {
    ThunderbirdTheme {
        IncomingServerSettingsScreen(
            onNext = {},
            onBack = {},
            viewModel = IncomingServerSettingsViewModel(
                mode = InteractionMode.Create,
                validator = IncomingServerSettingsValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}
