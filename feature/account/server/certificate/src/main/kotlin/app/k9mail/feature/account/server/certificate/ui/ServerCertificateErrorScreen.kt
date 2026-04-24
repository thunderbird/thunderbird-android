package app.k9mail.feature.account.server.certificate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Effect
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Event
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.ViewModel
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.thundermail.ui.brandBackground
import net.thunderbird.feature.thundermail.ui.component.template.ThundermailScaffold
import org.koin.androidx.compose.koinViewModel

@Composable
fun SharedTransitionScope.ServerCertificateErrorScreen(
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

    ThundermailScaffold(
        toolbar = {},
        bottomBar = { paddingValues, containerColor ->
            ButtonBar(
                state = state.value,
                dispatch = dispatch,
                modifier = Modifier
                    .imePadding()
                    .background(containerColor)
                    .padding(paddingValues),
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.navigationBars),
    ) { scaffoldPaddingValues, responsivePaddingValues, maxWidth ->
        ServerCertificateErrorContent(
            contentPadding = responsivePaddingValues,
            state = state.value,
            scrollState = scrollState,
            maxWidth = maxWidth,
            modifier = Modifier
                .fillMaxSize()
                .brandBackground()
                .padding(scaffoldPaddingValues)
                .consumeWindowInsets(scaffoldPaddingValues),
        )
    }
}

@Composable
private fun ButtonBar(
    state: State,
    dispatch: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(
                start = MainTheme.spacings.double,
                end = MainTheme.spacings.double,
                top = MainTheme.spacings.half,
                bottom = MainTheme.spacings.half,
            )
            .animateContentSize(),
    ) {
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
