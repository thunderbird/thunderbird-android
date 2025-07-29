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
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.icon.outlined.Warning
import app.k9mail.core.ui.compose.designsystem.organism.banner.global.BannerGlobalNotificationCard
import app.k9mail.core.ui.compose.theme2.MainTheme

@PreviewLightDark
@Composable
private fun BannerGlobalNotificationCardStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BannerGlobalNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Warning) },
                text = "Offline. No internet connection found.",
                action = {
                    ButtonText(
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
private fun BannerGlobalNotificationCardAnnotatedStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BannerGlobalNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Warning) },
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Black)) {
                        append("Offline. ")
                    }
                    append("No internet connection found.")
                },
                action = {
                    ButtonText(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = MainTheme.spacings.quadruple),
            )
        }
    }
}
