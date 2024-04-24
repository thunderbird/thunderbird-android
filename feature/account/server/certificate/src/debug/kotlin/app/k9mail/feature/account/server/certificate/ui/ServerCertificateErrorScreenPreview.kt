package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevices
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.certificate.data.InMemoryServerCertificateErrorRepository
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.usecase.FormatServerCertificateError
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

@Composable
@PreviewDevices
internal fun ServerCertificateErrorScreenPreview() {
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

    koinPreview {
        factory<ServerNameFormatter> { DefaultServerNameFormatter() }
        factory<FingerprintFormatter> { DefaultFingerprintFormatter() }
    } WithContent {
        PreviewWithTheme {
            ServerCertificateErrorScreen(
                onCertificateAccepted = {},
                onBack = {},
                viewModel = ServerCertificateErrorViewModel(
                    addServerCertificateException = { _, _, _ -> },
                    certificateErrorRepository = InMemoryServerCertificateErrorRepository(serverCertificateError),
                    formatServerCertificateError = FormatServerCertificateError(),
                    initialState = ServerCertificateErrorContract.State(isShowServerCertificate = false),
                ),
            )
        }
    }
}
