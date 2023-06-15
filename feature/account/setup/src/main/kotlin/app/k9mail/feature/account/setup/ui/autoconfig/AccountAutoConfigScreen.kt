package app.k9mail.feature.account.setup.ui.autoconfig

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Effect
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.Event
import app.k9mail.feature.account.setup.ui.autoconfig.AccountAutoConfigContract.ViewModel
import app.k9mail.feature.account.setup.ui.common.AccountSetupBottomBar
import app.k9mail.feature.account.setup.ui.common.AccountSetupTopHeader

@Composable
internal fun AccountAutoConfigScreen(
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

    Scaffold(
        topBar = {
            AccountSetupTopHeader()
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
        AccountAutoConfigContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigScreenK9Preview() {
    K9Theme {
        AccountAutoConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountAutoConfigViewModel(
                validator = AccountAutoConfigValidator(),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoConfigScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountAutoConfigScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountAutoConfigViewModel(
                validator = AccountAutoConfigValidator(),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
            ),
        )
    }
}
