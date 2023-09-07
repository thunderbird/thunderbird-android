package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.common.ui.preview.PreviewAccountStateRepository
import app.k9mail.feature.account.oauth.ui.preview.PreviewAccountOAuthViewModel
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ViewModel

@Composable
internal fun AccountAutoDiscoveryScreen(
    onNext: (isAutomaticConfig: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.NavigateNext -> onNext(effect.isAutomaticConfig)
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AppTitleTopHeader(
                title = stringResource(id = R.string.account_setup_title),
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(showNext = state.value.isNextButtonVisible),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountAutoDiscoveryContent(
            state = state.value,
            onEvent = { dispatch(it) },
            oAuthViewModel = viewModel.oAuthViewModel,
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoDiscoveryScreenK9Preview() {
    K9Theme {
        AccountAutoDiscoveryScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountAutoDiscoveryViewModel(
                validator = AccountAutoDiscoveryValidator(),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
                accountStateRepository = PreviewAccountStateRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountAutoDiscoveryScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountAutoDiscoveryScreen(
            onNext = { },
            onBack = {},
            viewModel = AccountAutoDiscoveryViewModel(
                validator = AccountAutoDiscoveryValidator(),
                getAutoDiscovery = { AutoDiscoveryResult.NoUsableSettingsFound },
                accountStateRepository = PreviewAccountStateRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}
