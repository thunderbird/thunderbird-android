package app.k9mail.feature.account.server.config.ui.outgoing

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
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract.Effect
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract.ViewModel

@Composable
fun AccountOutgoingConfigScreen(
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
            AccountTopAppBar(
                title = stringResource(id = R.string.account_server_config_outgoing_top_bar_title),
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
        AccountOutgoingConfigContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingConfigScreenK9Preview() {
    K9Theme {
        AccountOutgoingConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountOutgoingConfigViewModel(
                validator = AccountOutgoingConfigValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingConfigScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountOutgoingConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountOutgoingConfigViewModel(
                validator = AccountOutgoingConfigValidator(),
                accountStateRepository = PreviewAccountStateRepository(),
            ),
        )
    }
}
