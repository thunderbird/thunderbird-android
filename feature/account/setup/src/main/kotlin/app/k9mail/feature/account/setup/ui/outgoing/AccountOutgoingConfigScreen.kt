package app.k9mail.feature.account.setup.ui.outgoing

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopAppBar
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.Event
import app.k9mail.feature.account.setup.ui.outgoing.AccountOutgoingConfigContract.ViewModel

@Composable
internal fun AccountOutgoingConfigScreen(
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

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AccountSetupTopAppBar(
                title = stringResource(id = R.string.account_setup_outgoing_config_top_bar_title),
            )
        },
        bottomBar = {
            AccountSetupBottomBar(
                nextButtonText = stringResource(id = R.string.account_setup_button_next),
                backButtonText = stringResource(id = R.string.account_setup_button_back),
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
            ),
        )
    }
}
