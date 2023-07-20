package app.k9mail.feature.account.oauth.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.oauth.R
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Effect
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.Event
import app.k9mail.feature.account.oauth.ui.AccountOAuthContract.ViewModel

@Composable
fun AccountOAuthScreen(
    onOAuthResult: (OAuthResult) -> Unit,
    viewModel: ViewModel,
    modifier: Modifier = Modifier,
) {
    val oAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.event(Event.OnOAuthResult(it.resultCode, it.data))
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateNext -> onOAuthResult(OAuthResult.Success(effect.state))
            is Effect.NavigateBack -> onOAuthResult(OAuthResult.Failure)
            is Effect.LaunchOAuth -> oAuthLauncher.launch(effect.intent)
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        topBar = {
            AppTitleTopHeader(stringResource(id = R.string.account_oauth_title))
        },
        bottomBar = {
            WizardNavigationBar(
                state = state.value.wizardNavigationBarState,
                nextButtonText = stringResource(id = R.string.account_oauth_button_next),
                backButtonText = stringResource(id = R.string.account_oauth_button_back),
                onNextClick = { },
                onBackClick = { dispatch(Event.OnBackClicked) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        AccountOAuthContent(
            state = state.value,
            onEvent = { dispatch(it) },
            contentPadding = innerPadding,
        )
    }
}
