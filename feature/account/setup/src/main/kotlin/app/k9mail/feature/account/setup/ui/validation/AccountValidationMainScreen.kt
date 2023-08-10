package app.k9mail.feature.account.setup.ui.validation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.oauth.ui.preview.PreviewAccountOAuthViewModel
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.data.InMemoryCertificateErrorRepository
import app.k9mail.feature.account.setup.ui.preview.PreviewAccountSetupStateRepository
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.ViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult

@Composable
internal fun AccountValidationMainScreen(
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val dispatch = { event: Event -> viewModel.event(event) }

    Scaffold(
        topBar = {
            AppTitleTopHeader(title = stringResource(id = R.string.account_setup_title))
        },
        bottomBar = {
            WizardNavigationBar(
                nextButtonText = stringResource(id = R.string.account_setup_button_next),
                backButtonText = stringResource(id = R.string.account_setup_button_back),
                onNextClick = { dispatch(Event.OnNextClicked) },
                onBackClick = { dispatch(Event.OnBackClicked) },
                state = WizardNavigationBarState(
                    showNext = state.value.isSuccess,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountValidationContent(
            onEvent = { dispatch(it) },
            state = state.value,
            isIncomingValidation = viewModel.isIncomingValidation,
            oAuthViewModel = viewModel.oAuthViewModel,
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingValidationScreenK9Preview() {
    K9Theme {
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
                    ServerSettingsValidationResult.Success
                },
                accountSetupStateRepository = PreviewAccountSetupStateRepository(),
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryCertificateErrorRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
                isIncomingValidation = true,
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingValidationScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
                    ServerSettingsValidationResult.Success
                },
                accountSetupStateRepository = PreviewAccountSetupStateRepository(),
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryCertificateErrorRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
                isIncomingValidation = true,
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingValidationScreenK9Preview() {
    K9Theme {
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
                    ServerSettingsValidationResult.Success
                },
                accountSetupStateRepository = PreviewAccountSetupStateRepository(),
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryCertificateErrorRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
                isIncomingValidation = false,
            ),
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountOutgoingValidationScreenThunderbirdPreview() {
    ThunderbirdTheme {
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
                    ServerSettingsValidationResult.Success
                },
                accountSetupStateRepository = PreviewAccountSetupStateRepository(),
                authorizationStateRepository = { true },
                certificateErrorRepository = InMemoryCertificateErrorRepository(),
                oAuthViewModel = PreviewAccountOAuthViewModel(),
                isIncomingValidation = false,
            ),
        )
    }
}
