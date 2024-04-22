package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.baseline.withBaseline
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.common.resources.annotatedStringResource
import app.k9mail.core.ui.compose.common.text.bold
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconsWithBaseline
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadlineMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveWidthContainer
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.domain.entity.FormattedServerCertificateError
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import app.k9mail.feature.account.server.certificate.ui.ServerCertificateErrorContract.State
import okio.ByteString.Companion.decodeHex
import org.koin.compose.koinInject

@Composable
internal fun ServerCertificateErrorContent(
    innerPadding: PaddingValues,
    state: State,
    scrollState: ScrollState,
) {
    ResponsiveWidthContainer(modifier = Modifier.padding(innerPadding)) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            CertificateErrorOverview(state)

            AnimatedContent(
                targetState = state.isShowServerCertificate,
                label = "ServerCertificateViewVisibility",
            ) { isShowServerCertificate ->
                if (isShowServerCertificate) {
                    ServerCertificateView(
                        serverCertificateProperties = state.certificateError!!.serverCertificateProperties,
                    )
                }
            }
        }
    }
}

@Composable
private fun CertificateErrorOverview(state: State) {
    Column(
        modifier = Modifier.padding(all = MainTheme.spacings.double),
    ) {
        WarningTitle()
        TextTitleMedium(stringResource(R.string.account_server_certificate_unknown_error_subtitle))

        Spacer(modifier = Modifier.height(MainTheme.spacings.quadruple))

        state.certificateError?.let { certificateError ->
            CertificateErrorDescription(certificateError)
        }
    }
}

@Composable
private fun WarningTitle() {
    Row {
        val warningIcon = IconsWithBaseline.Filled.warning
        val iconSize = MainTheme.sizes.medium
        val iconScalingFactor = iconSize / warningIcon.image.defaultHeight
        val iconBaseline = warningIcon.baseline * iconScalingFactor

        Icon(
            imageVector = warningIcon.image,
            tint = MainTheme.colors.warning,
            modifier = Modifier
                .padding(end = MainTheme.spacings.default)
                .requiredSize(iconSize)
                .withBaseline(iconBaseline)
                .alignByBaseline(),
        )
        TextHeadlineMedium(
            text = stringResource(R.string.account_server_certificate_warning_title),
            modifier = Modifier.alignByBaseline(),
        )
    }
}

@Composable
private fun CertificateErrorDescription(
    certificateError: FormattedServerCertificateError,
    serverNameFormatter: ServerNameFormatter = koinInject(),
) {
    TextBodyLarge(
        text = annotatedStringResource(
            id = R.string.account_server_certificate_unknown_error_description_format,
            serverNameFormatter.format(certificateError.hostname).bold(),
        ),
    )
}

@Composable
@Preview(showBackground = true)
internal fun ServerCertificateErrorContentPreview() {
    val state = State(
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
        K9Theme {
            ServerCertificateErrorContent(
                innerPadding = PaddingValues(all = 0.dp),
                state = state,
                scrollState = rememberScrollState(),
            )
        }
    }
}
