package app.k9mail.feature.account.setup.ui.validation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import app.k9mail.feature.account.setup.R
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Effect
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.Event
import app.k9mail.feature.account.setup.ui.validation.AccountValidationContract.ViewModel
import com.fsck.k9.mail.server.ServerSettingsValidationResult

@Composable
internal fun AccountValidationScreen(
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

    LaunchedEffect(key1 = Unit) {
        dispatch(Event.ValidateServerSettings)
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

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
            contentPadding = innerPadding,
        )
    }
}

@Composable
@DevicePreviews
internal fun AccountIncomingConfigScreenK9Preview() {
    K9Theme {
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
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
        AccountValidationScreen(
            onNext = {},
            onBack = {},
            viewModel = AccountValidationViewModel(
                validateServerSettings = {
                    ServerSettingsValidationResult.Success
                },
            ),
        )
    }
}
