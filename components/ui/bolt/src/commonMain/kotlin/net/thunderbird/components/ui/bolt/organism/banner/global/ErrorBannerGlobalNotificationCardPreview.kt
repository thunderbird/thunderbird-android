package net.thunderbird.components.ui.bolt.organism.banner.global

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.theme.MainTheme

@PreviewLightDark
@Composable
private fun ErrorBannerGlobalNotificationCardStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ErrorBannerGlobalNotificationCard(
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
private fun ErrorBannerGlobalNotificationCardAnnotatedStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            ErrorBannerGlobalNotificationCard(
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
