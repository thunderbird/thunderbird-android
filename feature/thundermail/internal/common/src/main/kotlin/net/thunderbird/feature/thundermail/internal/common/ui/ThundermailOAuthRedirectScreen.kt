package net.thunderbird.feature.thundermail.internal.common.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.feature.account.common.ui.WizardNavigationBar
import app.k9mail.feature.account.common.ui.WizardNavigationBarState
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.thundermail.internal.common.R
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute
import org.koin.androidx.compose.koinViewModel

@Composable
internal fun ThundermailOAuthRedirectScreen(
    onBack: () -> Unit,
    onFinish: (ThundermailRoute) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThundermailContract.ViewModel = koinViewModel<ThundermailContract.ViewModel>(),
) {
    val oAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.event(ThundermailContract.Event.OnOAuthResult(it.resultCode, it.data))
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is ThundermailContract.Effect.LaunchOAuth -> oAuthLauncher.launch(effect.intent)
            is ThundermailContract.Effect.NavigateToIncomingServerSettings ->
                onFinish(ThundermailRoute.IncomingSettings)
        }
    }

    var launchedOAuth by remember { mutableStateOf(false) }

    LaunchedEffect(state.value.initialized) {
        if (state.value.initialized && !launchedOAuth) {
            dispatch(ThundermailContract.Event.SignInClicked)
            launchedOAuth = true
        }
    }

    LaunchedErrorEffect(state.value, onBack)

    ThundermailOAuthRedirectScreen(state = state.value, onBack = onBack, modifier = modifier)
}

@Composable
internal fun ThundermailOAuthRedirectScreen(
    state: ThundermailContract.State,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        bottomBar = {
            WizardNavigationBar(
                onNextClick = {},
                onBackClick = onBack,
                state = WizardNavigationBarState(showNext = false, showBack = state.error != null),
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .safeContentPadding()
                .padding(horizontal = MainTheme.spacings.quadruple),
        ) {
            Crossfade(
                targetState = state.error,
                modifier = Modifier.align(Alignment.Center),
            ) { error ->
                if (error == null) {
                    RedirectingUserContent(modifier = Modifier.fillMaxWidth())
                } else {
                    ErrorDetails(error, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LaunchedErrorEffect(
    state: ThundermailContract.State,
    onBack: () -> Unit,
) {
    LaunchedEffect(state.error) {
        when (state.error) {
            ThundermailContract.Error.Canceled -> onBack()
            else -> Unit
        }
    }
}

@Composable
private fun RedirectingUserContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(MainTheme.sizes.medium),
            )
        }
        TextBodyLarge(stringResource(R.string.thundermail_redirecting))
    }
}

@Composable
private fun ErrorDetails(error: ThundermailContract.Error, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
    ) {
        TextTitleLarge(text = stringResource(R.string.thundermail_something_went_wrong))
        TextBodyLarge(
            text = when (error) {
                ThundermailContract.Error.BrowserNotAvailable ->
                    stringResource(R.string.thundermail_browser_is_not_available)

                ThundermailContract.Error.Canceled -> stringResource(R.string.thundermail_operation_was_canceled)

                is ThundermailContract.Error.Unknown ->
                    stringResource(R.string.thundermail_unknown_error_please_consider_reporting)
            },
        )
    }
}
