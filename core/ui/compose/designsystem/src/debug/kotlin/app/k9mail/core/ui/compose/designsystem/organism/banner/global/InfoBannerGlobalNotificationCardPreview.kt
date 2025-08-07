package app.k9mail.core.ui.compose.designsystem.organism.banner.global

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.InfoBannerGlobalNotificationCard
import app.k9mail.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun InfoBannerGlobalNotificationCardStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfoBannerGlobalNotificationCard(
                text = "Offline. No internet connection found.",
                action = {
                    NotificationActionButton(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerGlobalNotificationCardNoActionPreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfoBannerGlobalNotificationCard(
                text = "Offline. No internet connection found.",
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerGlobalNotificationCardLongTextPreview(
    @PreviewParameter(LoremIpsum::class) text: String,
) {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfoBannerGlobalNotificationCard(
                text = text,
                action = {
                    NotificationActionButton(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerGlobalNotificationCardLongNoActionTextPreview(
    @PreviewParameter(LoremIpsum::class) text: String,
) {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfoBannerGlobalNotificationCard(
                text = text,
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerGlobalNotificationCardAnnotatedStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            InfoBannerGlobalNotificationCard(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                        append("Offline. ")
                    }
                    append("No internet connection found.")
                },
                action = {
                    NotificationActionButton(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}
