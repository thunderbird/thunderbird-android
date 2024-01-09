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
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline6
import app.k9mail.core.ui.compose.designsystem.atom.text.TextOverline
import app.k9mail.core.ui.compose.designsystem.atom.text.TextSubtitle2
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties

@Composable
fun ServerCertificateView(
    serverCertificateProperties: ServerCertificateProperties,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            start = MainTheme.spacings.double,
            end = MainTheme.spacings.double,
            top = MainTheme.spacings.double,
        ),
    ) {
        TextHeadline6(stringResource(R.string.account_server_certificate_section_title))
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_subject_alternative_names))
        for (subjectAlternativeName in serverCertificateProperties.subjectAlternativeNames) {
            BulletedListItem(subjectAlternativeName)
        }

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_not_valid_before))
        TextBody1(text = serverCertificateProperties.notValidBefore)

        Spacer(modifier = Modifier.height(MainTheme.spacings.default))

        TextSubtitle2(stringResource(R.string.account_server_certificate_not_valid_after))
        TextBody1(text = serverCertificateProperties.notValidAfter)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_subject))
        TextBody1(text = serverCertificateProperties.subject)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(stringResource(R.string.account_server_certificate_issuer))
        TextBody1(text = serverCertificateProperties.issuer)

        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextOverline(text = stringResource(R.string.account_server_certificate_fingerprints_section))
        Spacer(modifier = Modifier.height(MainTheme.spacings.default))

        TextSubtitle2(text = "SHA-1")
        TextBody1(text = serverCertificateProperties.fingerprintSha1)
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(text = "SHA-256")
        TextBody1(text = serverCertificateProperties.fingerprintSha256)
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))

        TextSubtitle2(text = "SHA-512")
        TextBody1(text = serverCertificateProperties.fingerprintSha512)
        Spacer(modifier = Modifier.height(MainTheme.spacings.double))
    }
}

@Composable
private fun BulletedListItem(text: String) {
    Row {
        TextBody1(
            text = "\u2022",
            modifier = Modifier.padding(horizontal = MainTheme.spacings.half),
        )
        TextBody1(text = text)
    }
}

@Composable
@Preview(showBackground = true)
internal fun ServerCertificateViewPreview() {
    val serverCertificateProperties = ServerCertificateProperties(
        subjectAlternativeNames = listOf("*.domain.example", "domain.example"),
        notValidBefore = "January 1, 2023, 12:00 AM",
        notValidAfter = "December 31, 2023, 11:59 PM",
        subject = "CN=*.domain.example",
        issuer = "CN=test, O=MZLA",
        fingerprintSha1 = "33ab5639bfd8e7b95eb1d8d0b87781d4ffea4d5d",
        fingerprintSha256 = "1894a19c85ba153acbf743ac4e43fc004c891604b26f8c69e1e83ea2afc7c48f",
        fingerprintSha512 = "81381f1dacd4824a6c503fd07057763099c12b8309d0abcec4000c9060cbbfa67988b2ada669ab4837fcd3d4" +
            "ea6e2b8db2b9da9197d5112fb369fd006da545de",
    )

    K9Theme {
        ServerCertificateView(
            serverCertificateProperties = serverCertificateProperties,
        )
    }
}
