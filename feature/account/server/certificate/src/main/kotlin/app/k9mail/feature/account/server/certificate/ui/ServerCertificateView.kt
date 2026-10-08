package app.k9mail.feature.account.server.certificate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.k9mail.feature.account.server.certificate.R
import app.k9mail.feature.account.server.certificate.domain.entity.ServerCertificateProperties
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleSmall
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import okio.ByteString
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
            start = BoltTheme.spacings.double,
            end = BoltTheme.spacings.double,
            top = BoltTheme.spacings.double,
        ),
    ) {
        TextTitleLarge(stringResource(R.string.account_server_certificate_section_title))
        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))

        if (serverCertificateProperties.subjectAlternativeNames.isNotEmpty()) {
            TextTitleSmall(stringResource(R.string.account_server_certificate_subject_alternative_names))
            for (subjectAlternativeName in serverCertificateProperties.subjectAlternativeNames) {
                BulletedListItem(serverNameFormatter.format(subjectAlternativeName))
            }

            Spacer(modifier = Modifier.height(BoltTheme.spacings.double))
        }

        TextTitleSmall(stringResource(R.string.account_server_certificate_not_valid_before))
        TextBodyLarge(text = serverCertificateProperties.notValidBefore)

        Spacer(modifier = Modifier.height(BoltTheme.spacings.default))

        TextTitleSmall(stringResource(R.string.account_server_certificate_not_valid_after))
        TextBodyLarge(text = serverCertificateProperties.notValidAfter)

        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))

        TextTitleSmall(stringResource(R.string.account_server_certificate_subject))
        TextBodyLarge(text = serverCertificateProperties.subject)

        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))

        TextTitleSmall(stringResource(R.string.account_server_certificate_issuer))
        TextBodyLarge(text = serverCertificateProperties.issuer)

        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))

        TextLabelSmall(text = stringResource(R.string.account_server_certificate_fingerprints_section))
        Spacer(modifier = Modifier.height(BoltTheme.spacings.default))

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
        separatorColor = BoltTheme.colors.onSurfaceVariant,
    )

    Column {
        TextTitleSmall(text = title)
        TextBodyLarge(text = formattedFingerprint)
        Spacer(modifier = Modifier.height(BoltTheme.spacings.double))
    }
}

@Composable
private fun BulletedListItem(text: String) {
    Row {
        TextBodyLarge(
            text = "\u2022",
            modifier = Modifier.padding(horizontal = BoltTheme.spacings.half),
        )
        TextBodyLarge(text = text)
    }
}
