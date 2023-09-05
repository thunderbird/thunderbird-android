package app.k9mail.feature.account.server.certificate.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.common.mvi.observe
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline4
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle1
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.designsystem.template.Scaffold
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Effect
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.Event
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.ViewModel
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import org.koin.androidx.compose.koinViewModel

// Note: This is a placeholder with mostly hardcoded text.
// TODO: Replace with final design.
@Composable
fun ServerCertificateErrorScreen(
    onCertificateAccepted: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewModel = koinViewModel<ServerCertificateErrorViewModel>(),
) {
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
            ResponsiveWidthContainer(
                modifier = Modifier
                    .padding(start = MainTheme.spacings.double, end = MainTheme.spacings.double),
            ) {
                Column {
                    Button(
                        text = "Go back (recommended)",
                        onClick = { dispatch(Event.OnBackClicked) },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                    ButtonOutlined(
                        text = "Accept consequences and continue",
                        onClick = { dispatch(Event.OnCertificateAcceptedClicked) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        ResponsiveWidthContainer(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(all = MainTheme.spacings.double),
            ) {
                TextHeadline4(text = "Warning")
                TextSubtitle1("Invalid certificate")

                Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))

                TextBody1(
                    text = "The server presented an invalid TLS certificate. " +
                        "Sometimes, this is because of a server misconfiguration. " +
                        "Sometimes it is because someone is trying to attack you or your mail server. " +
                        "If you're not sure what's up, click \"Go back\" and contact the folks who manage your " +
                        "mail server.",
                )

                Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))

                TextBody1(state.value.errorText)
            }
        }
    }
}

@Composable
@DevicePreviews
internal fun ServerCertificateErrorScreenK9Preview() {
    val inputStream = """
        -----BEGIN CERTIFICATE-----
        MIIE8jCCA9qgAwIBAgISA3bsPKY1eoe/RiBO2t8fUvh1MA0GCSqGSIb3DQEBCwUA
        MDIxCzAJBgNVBAYTAlVTMRYwFAYDVQQKEw1MZXQncyBFbmNyeXB0MQswCQYDVQQD
        EwJSMzAeFw0yMzA3MjEyMDU1MTJaFw0yMzEwMTkyMDU1MTFaMBcxFTATBgNVBAMM
        DCouYmFkc3NsLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJgw
        o/dYmPaujmm7sqIuZCe5/kyMwDYKo/pWeeXSvQxRXhxiVvd2Xu9PG0ZXW2R0xOSr
        BpaRWm6MXxEnNqNr+n22j9US6M62zJpcuU4tQ0J8xRyIGL6rM53z59rEnCdkF9HQ
        +7y7PBlVXCm0jrw51h3Bg5qryvTFyimIbqGw0UJhM7m/NaVJWZyBRwHp7emXxRJC
        kC7pdX462c+m/7rQ06iohqUt6mf0DkUH1QjpaVbZm8CBs/GSiLB3LdMHj1uvrXgH
        z8dp0nQ3eVRCjuD1xVcZnFoeEa/W3a9ZdcBj1phr9XOwaqYMeAv64g2w40G6fXMH
        9DpHuFarRtleQusiPAMCAwEAAaOCAhswggIXMA4GA1UdDwEB/wQEAwIFoDAdBgNV
        HSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDAYDVR0TAQH/BAIwADAdBgNVHQ4E
        FgQU1M4J2vX/9DWJnsAtofmT+94js/YwHwYDVR0jBBgwFoAUFC6zF7dYVsuuUAlA
        5h+vnYsUwsYwVQYIKwYBBQUHAQEESTBHMCEGCCsGAQUFBzABhhVodHRwOi8vcjMu
        by5sZW5jci5vcmcwIgYIKwYBBQUHMAKGFmh0dHA6Ly9yMy5pLmxlbmNyLm9yZy8w
        IwYDVR0RBBwwGoIMKi5iYWRzc2wuY29tggpiYWRzc2wuY29tMBMGA1UdIAQMMAow
        CAYGZ4EMAQIBMIIBBQYKKwYBBAHWeQIEAgSB9gSB8wDxAHYAtz77JN+cTbp18jnF
        ulj0bF38Qs96nzXEnh0JgSXttJkAAAGJenMebAAABAMARzBFAiAH7A3OWC1AKOcO
        jsOP39nzkyoIdrwYFHOOW1qKkLrk9gIhAJD0xFn5FwJvag3K6mTXAlW1EvIy9joA
        okiPniKVBIztAHcAejKMVNi3LbYg6jjgUh7phBZwMhOFTTvSK8E6V6NS61IAAAGJ
        enMehwAABAMASDBGAiEAvRyLnINSJQ0WyfcU8L0PY5z7//Gq8P9i2HJvZJvnfBkC
        IQCHslQMJaOg+rn9+2WW4KKgYW/yDrvBbiVABW5CcYWR0DANBgkqhkiG9w0BAQsF
        AAOCAQEAB/JpXHqRnGmCFz3f0hx7mJYY/auSNWnOgpdRpc3JXzcOHHUd+569UGtu
        TSMAFEGNXYTbXrG52iGBCrdfe1kkRokg7/KtUvFRelkoNt4FN/4/zVjBxINXVIMb
        /7toq4OxBF/sz4SU+eXanmwJyOMmNQzM94zqDwrEmMNuNLYshdWn7XyJCXIM4X+6
        8M/anh/pi2AviLHH9pszkeuH3AjGJR68cPf+QKC4XcFloR08fhx0jKl8LBa4A6Nm
        o7IlPgdD9rzZCsbYe+VNBQWY3358u7ifOJG8r2jXzyHKgUC+OBXgz3kjrClzJfl1
        pjcJhNU1UQtIVERwmxI9F5oQqUyxvA==
        -----END CERTIFICATE-----
    """.trimIndent().byteInputStream()

    val certificateFactory = CertificateFactory.getInstance("X.509")
    val certificate = certificateFactory.generateCertificate(inputStream) as X509Certificate

    val serverCertificateError = ServerCertificateError(
        hostname = "mail.domain.example",
        port = 143,
        certificateChain = listOf(certificate),
    )

    K9Theme {
        ServerCertificateErrorScreen(
            onCertificateAccepted = {},
            onBack = {},
            viewModel = ServerCertificateErrorViewModel(
                addServerCertificateException = { _, _, _ -> },
                certificateErrorRepository = InMemoryServerCertificateErrorRepository(serverCertificateError),
            ),
        )
    }
}
