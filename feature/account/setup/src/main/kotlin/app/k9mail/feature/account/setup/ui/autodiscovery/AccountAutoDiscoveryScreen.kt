package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.preview.PreviewAccountStateRepository
import app.k9mail.feature.account.oauth.ui.preview.PreviewAccountOAuthViewModel
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ViewModel

@Composable
internal fun AccountAutoDiscoveryScreen(
    onNext: (AutoDiscoveryUiResult) -> Unit,
    onBack: () -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.NavigateNext -> onNext(effect.result)
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    AccountAutoDiscoveryContent(
        state = state.value,
        onEvent = { dispatch(it) },
        oAuthViewModel = viewModel.oAuthViewModel,
        modifier = modifier,
    )
}

@Composable
@PreviewDevicesWithBackground
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
@PreviewDevicesWithBackground
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
