package app.k9mail.feature.account.server.validation.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.mvi.observeWithoutEffect
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.ViewModel
import net.thunderbird.core.common.provider.BrandNameProvider

@Composable
internal fun ServerValidationMainScreen(
    viewModel: ViewModel,
    brandNameProvider: BrandNameProvider,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observeWithoutEffect()

    Scaffold(
        topBar = {
            AppTitleTopHeader(
                title = brandNameProvider.brandName,
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    showNext = false,
                ),
                modifier = Modifier.imePadding(),
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        ServerValidationContent(
            onEvent = { dispatch(it) },
            state = state.value,
            isIncomingValidation = viewModel.isIncomingValidation,
            oAuthViewModel = viewModel.oAuthViewModel,
            contentPadding = innerPadding,
        )
    }
}
