package app.k9mail.feature.account.server.config.ui.incoming

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AccountTopAppBar
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.preview.PreviewAccountStateRepository
import app.k9mail.feature.account.server.config.R
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract.ViewModel

@Composable
fun AccountIncomingConfigScreen(
    onNext: (AccountIncomingConfigContract.State) -> Unit,
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
            AccountTopAppBar(
                title = stringResource(id = R.string.account_server_config_incoming_top_bar_title),
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountIncomingConfigContent(
            onEvent = { dispatch(it) },
            state = state.value,
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigScreenK9Preview() {
    K9Theme {
        AccountIncomingConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountIncomingConfigViewModel(
                validator = AccountIncomingConfigValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountIncomingConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountIncomingConfigViewModel(
                validator = AccountIncomingConfigValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}
