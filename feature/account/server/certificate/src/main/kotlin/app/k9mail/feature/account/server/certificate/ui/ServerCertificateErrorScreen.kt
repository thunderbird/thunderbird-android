package app.k9mail.feature.account.server.certificate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Effect
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Event
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.ViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ServerCertificateErrorScreen(
    onCertificateAccepted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ServerCertificateErrorViewModel>(),
) {
    val scrollState = rememberScrollState()

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is Effect.NavigateCertificateAccepted -> onCertificateAccepted()
            is Effect.NavigateBack -> onBack()
        }
    }

    BackHandler {
        dispatch(Event.OnBackClicked)
    }

    Scaffold(
        bottomBar = {
            ButtonBar(
                state = state.value,
                dispatch = dispatch,
                scrollState = scrollState,
                modifier = Modifier.imePadding(),
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { innerPadding ->
        ServerCertificateErrorContent(
            innerPadding = innerPadding,
            state = state.value,
            scrollState = scrollState,
        )
    }
}

@Composable
private fun ButtonBar(
    state: State,
    dispatch: (Event) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val elevation by animateDpAsState(
        targetValue = if (scrollState.canScrollForward) 8.dp else 0.dp,
        label = "BottomBarElevation",
    )

    Surface(
        tonalElevation = elevation,
        modifier = modifier,
    ) {
        ResponsiveWidthContainer(
            modifier = Modifier
                .padding(
                    start = MainTheme.spacings.double,
                    end = MainTheme.spacings.double,
                    top = MainTheme.spacings.half,
                    bottom = MainTheme.spacings.half,
                ),
        ) { contentPadding ->
            Column(modifier = Modifier.animateContentSize().padding(contentPadding)) {
                ButtonFilled(
                    text = stringResource(R.string.account_server_certificate_button_back),
                    onClick = { dispatch(Event.OnBackClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Crossfade(
                    targetState = state.isShowServerCertificate,
                    label = "ContinueButton",
                ) { isShowServerCertificate ->
                    if (isShowServerCertificate) {
                        ButtonOutlined(
                            text = stringResource(R.string.account_server_certificate_button_continue),
                            onClick = { dispatch(Event.OnCertificateAcceptedClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        ButtonOutlined(
                            text = stringResource(R.string.account_server_certificate_button_advanced),
                            onClick = { dispatch(Event.OnShowAdvancedClicked) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
