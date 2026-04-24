package app.k9mail.feature.account.server.validation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.server.validation.R
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observeWithoutEffect
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold

@Composable
internal fun ServerValidationMainScreen(
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observeWithoutEffect()

    val lazyListState = rememberLazyListState()
    ThundermailScaffold(
        header = { paddingValues ->
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.double)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        subHeaderText = stringResource(
            R.string.account_server_validation_title,
            stringResource(
                if (viewModel.isIncomingValidation) {
                    R.string.account_server_validation_incoming_server
                } else {
                    R.string.account_server_validation_outgoing_server
                },
            ),
        ),
        bottomBar = { paddingValues, containerColor ->
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    showNext = false,
                ),
                modifier = Modifier
                    .imePadding()
                    .background(containerColor)
                    .padding(paddingValues)
                    .padding(top = MainTheme.spacings.default)
                    .padding(horizontal = MainTheme.spacings.quadruple),
            )
        },
        canScrollForward = lazyListState.canScrollForward,
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        ServerValidationContent(
            onEvent = { dispatch(it) },
            state = state.value,
            isIncomingValidation = viewModel.isIncomingValidation,
            oAuthViewModel = viewModel.oAuthViewModel,
            listState = lazyListState,
            contentPadding = responsivePaddingValues,
            maxWidth = maxWidth,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .consumeWindowInsets(scaffoldPaddingValues)
                .imePadding(),
        )
    }
}
