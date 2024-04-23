package app.k9mail.feature.account.server.validation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.common.mvi.observeWithoutEffect
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.ui.preview.PreviewAccountOAuthViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.Event
import app.k9mail.feature.account.server.validation.ui.ServerValidationContract.ViewModel
import app.k9mail.feature.account.server.validation.ui.fake.FakeIncomingServerValidationViewModel

@Composable
internal fun ServerValidationToolbarScreen(
    title: String,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val (state, dispatch) = viewModel.observeWithoutEffect()

    Scaffold(
        topBar = {
            TopAppBarWithBackButton(
                title = title,
                onBackClick = { dispatch(Event.OnBackClicked) },
            )
        },
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    showNext = false,
                ),
            )
        },
        modifier = modifier,
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

@Composable
@PreviewDevices
internal fun IncomingServerValidationToolbarScreenK9Preview() {
    K9Theme {
        ServerValidationToolbarScreen(
            title = "Incoming server settings",
            viewModel = FakeIncomingServerValidationViewModel(
                oAuthViewModel = PreviewAccountOAuthViewModel(),
            ),
        )
    }
}
