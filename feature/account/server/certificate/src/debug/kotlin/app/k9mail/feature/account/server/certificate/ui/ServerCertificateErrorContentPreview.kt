package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import okio.ByteString.Companion.decodeHex

@Composable
@Preview(showBackground = true)
internal fun ServerCertificateErrorContentPreview() {
    val state = ServerCertificateErrorContract.State(
        isShowServerCertificate = true,
        certificateError = FormattedServerCertificateError(
            hostname = "mail.domain.example",
            serverCertificateProperties = ServerCertificateProperties(
                subjectAlternativeNames = listOf("*.domain.example", "domain.example"),
                notValidBefore = "January 1, 2023, 12:00 AM",
                notValidAfter = "December 31, 2023, 11:59 PM",
                subject = "CN=*.domain.example",
                issuer = "CN=test, O=MZLA",
                fingerprintSha1 = "33ab5639bfd8e7b95eb1d8d0b87781d4ffea4d5d".decodeHex(),
                fingerprintSha256 = "1894a19c85ba153acbf743ac4e43fc004c891604b26f8c69e1e83ea2afc7c48f".decodeHex(),
                fingerprintSha512 = (
                    "81381f1dacd4824a6c503fd07057763099c12b8309d0abcec4000c9060cbbfa6" +
                        "7988b2ada669ab4837fcd3d4ea6e2b8db2b9da9197d5112fb369fd006da545de"
                    ).decodeHex(),
            ),
        ),
    )

    koinPreview {
        factory<ServerNameFormatter> { DefaultServerNameFormatter() }
        factory<FingerprintFormatter> { DefaultFingerprintFormatter() }
    } WithContent {
        PreviewWithTheme {
            ServerCertificateErrorContent(
                innerPadding = PaddingValues(all = 0.dp),
                state = state,
                scrollState = rememberScrollState(),
            )
        }
    }
}
