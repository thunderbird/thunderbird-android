package app.k9mail.core.ui.compose.designsystem.organism.banner.inline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemesLightDark
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton

@PreviewLightDark
@Composable
private fun InfoBannerInlineNotificationCardPreviewPreview() {
    PreviewWithThemesLightDark {
        InfoBannerInlineNotificationCard(
            title = "Notification title",
            supportingText = "Supporting text",
            actions = {
                NotificationActionButton(text = "View support article", onClick = {}, isExternalLink = true)
                NotificationActionButton(text = "Action 1", onClick = {})
            },
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerInlineNotificationCardLongTextClippedPreviewPreview() {
    val title = remember { LoremIpsum(words = 20).values.joinToString(" ") }
    val supportingText = remember { LoremIpsum(words = 60).values.joinToString(" ") }
    PreviewWithThemesLightDark {
        InfoBannerInlineNotificationCard(
            title = title,
            supportingText = supportingText,
            actions = {
                NotificationActionButton(text = "View support article", onClick = {}, isExternalLink = true)
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            behaviour = BannerInlineNotificationCardBehaviour.Clipped,
        )
    }
}

@PreviewLightDark
@Composable
private fun InfoBannerInlineNotificationCardLongTextExpandedPreviewPreview() {
    val title = remember { LoremIpsum(words = 20).values.joinToString(" ") }
    val supportingText = remember { LoremIpsum(words = 60).values.joinToString(" ") }
    PreviewWithThemesLightDark {
        InfoBannerInlineNotificationCard(
            title = title,
            supportingText = supportingText,
            actions = {
                NotificationActionButton(text = "View support article", onClick = {}, isExternalLink = true)
                NotificationActionButton(text = "Action 1", onClick = {})
            },
            behaviour = BannerInlineNotificationCardBehaviour.Expanded,
        )
    }
}
