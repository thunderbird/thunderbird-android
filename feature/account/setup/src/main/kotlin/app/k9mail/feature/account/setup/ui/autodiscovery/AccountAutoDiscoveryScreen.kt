package app.k9mail.feature.account.setup.ui.autodiscovery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.AutoDiscoveryUiResult
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Effect
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.Event
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold

@Composable
internal fun SharedTransitionScope.AccountAutoDiscoveryScreen(
    onNext: (AutoDiscoveryUiResult) -> Unit,
    onBack: () -> Unit,
    onImportAccountNavigate: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            Effect.NavigateBack -> onBack()
            is Effect.NavigateNext -> onNext(effect.result)
            Effect.NavigateToImportAccount -> onImportAccountNavigate()
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    val scrollState = rememberScrollState()
    ThundermailScaffold(
        header = {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
                sharedTransitionScope = this,
                animatedVisibilityScope = animatedVisibilityScope,
            )
        },
        subHeaderText = stringResource(R.string.account_setup_discovery_add_email_account),
        bottomBar = { paddingValues, containerColor ->
            WizardNavigationBar(
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(showNext = state.value.isNextButtonVisible),
                modifier = Modifier
                    .imePadding()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(containerColor)
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.default)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        maxWidth = Dp.Unspecified,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        AccountAutoDiscoveryContent(
            state = state.value,
            onEvent = { dispatch(it) },
            oAuthViewModel = viewModel.oAuthViewModel,
            contentPadding = responsivePaddingValues,
            maxWidth = maxWidth,
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .consumeWindowInsets(scaffoldPaddingValues)
                .imePadding(),
        )
    }
}
