package app.k9mail.feature.account.setup.ui.incoming

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
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Effect
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.Event
import app.k9mail.feature.account.setup.ui.incoming.AccountIncomingConfigContract.ViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult

@Composable
internal fun AccountIncomingConfigScreen(
    onNext: () -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onNext()
            is Effect.NavigateBack -> onBack()
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AccountSetupTopAppBar(
                title = stringResource(id = R.string.account_setup_incoming_config_top_bar_title),
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
                checkIncomingServerConfig = { _, _ ->
                    ServerSettingsValidationResult.Success
                },
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
                checkIncomingServerConfig = { _, _ ->
                    ServerSettingsValidationResult.Success
                },
            ),
        )
    }
}
