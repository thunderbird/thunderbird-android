package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.common.koin.koinPreview
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextOverline
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle2
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleLarge
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.koin.compose.koinInject

@Composable
internal fun ServerCertificateView(
    serverCertificateProperties: ServerCertificateProperties,
    modifier: Modifier = Modifier,
    serverNameFormatter: ServerNameFormatter = koinInject(),
    fingerprintFormatter: FingerprintFormatter = koinInject(),
) {
    Column(
        modifier = modifier.padding(
            start = MainTheme.spacings.double,
            end = MainTheme.spacings.double,
            top = MainTheme.spacings.double,
        ),
    ) {
        TextTitleLarge(stringResource(R.string.account_server_certificate_section_title))
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        if (serverCertificateProperties.subjectAlternativeNames.isNotEmpty()) {
            TextSubtitle2(stringResource(R.string.account_server_certificate_subject_alternative_names))
            for (subjectAlternativeName in serverCertificateProperties.subjectAlternativeNames) {
                BulletedListItem(serverNameFormatter.format(subjectAlternativeName))
            }

            Spacer(modifier = Modifier.height(MainTheme.spacings.double))
        }

        TextSubtitle2(stringResource(R.string.account_server_certificate_not_valid_before))
        TextBodyLarge(text = serverCertificateProperties.notValidBefore)

        Spacer(modifier = Modifier.height(MainTheme.spacings.default))

        TextSubtitle2(stringResource(R.string.account_server_certificate_not_valid_after))
        TextBodyLarge(text = serverCertificateProperties.notValidAfter)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_subject))
        TextBodyLarge(text = serverCertificateProperties.subject)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_issuer))
        TextBodyLarge(text = serverCertificateProperties.issuer)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextOverline(text = stringResource(R.string.account_server_certificate_fingerprints_section))
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))

        Fingerprint("SHA-1", serverCertificateProperties.fingerprintSha1, fingerprintFormatter)
        Fingerprint("SHA-256", serverCertificateProperties.fingerprintSha256, fingerprintFormatter)
        Fingerprint("SHA-512", serverCertificateProperties.fingerprintSha512, fingerprintFormatter)
    }
}

@Composable
private fun Fingerprint(
    title: String,
    fingerprint: ByteString,
    fingerprintFormatter: FingerprintFormatter,
) {
    val formattedFingerprint = fingerprintFormatter.format(
        fingerprint,
        separatorColor = MainTheme.colors.onBackgroundSecondary,
    )

    Column {
        TextSubtitle2(text = title)
        TextBodyLarge(text = formattedFingerprint)
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))
    }
}

@Composable
private fun BulletedListItem(text: String) {
    Row {
        TextBodyLarge(
            text = "\u2022",
            modifier = Modifier.padding(horizontal = MainTheme.spacings.half),
        )
        TextBodyLarge(text = text)
    }
}

@Composable
@Preview(showBackground = true)
internal fun ServerCertificateViewPreview() {
    val serverCertificateProperties = ServerCertificateProperties(
        subjectAlternativeNames = listOf(
            "*.domain.example",
            "domain.example",
            "quite.the.long.domain.name.that.hopefully.exceeds.the.available.width.example",
        ),
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
    )

    koinPreview {
        factory<ServerNameFormatter> { DefaultServerNameFormatter() }
        factory<FingerprintFormatter> { DefaultFingerprintFormatter() }
    } WithContent {
        K9Theme {
            ServerCertificateView(
                serverCertificateProperties = serverCertificateProperties,
            )
        }
    }
}
