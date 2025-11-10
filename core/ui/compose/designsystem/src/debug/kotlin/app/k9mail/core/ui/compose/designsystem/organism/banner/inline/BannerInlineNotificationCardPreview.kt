package app.k9mail.core.ui.compose.designsystem.organism.banner.inline

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
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.designsystem.molecule.notification.NotificationActionButton
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardCustomTitleAndDescriptionPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = {
                    TextTitleMedium(text = "Authentication required")
                },
                supportingText = {
                    TextBodyMedium(text = "Sign in to authenticate username@domain3.example")
                },
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Sign in", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = MainTheme.spacings.quadruple,
                    horizontal = MainTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Missing encryption key",
                supportingText = "To dismiss this error, disable encryption for this account or ensure " +
                    "encryption key is available in openKeychain app.",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = MainTheme.spacings.quadruple,
                    horizontal = MainTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationCardAnnotatedStringPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MainTheme.colors.tertiaryContainer)) {
                        append("Missing encryption key")
                    }
                },
                supportingText = buildAnnotatedString {
                    append("To dismiss this error, ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                        append("disable encryption for this account or ensure encryption key is available")
                    }
                    append("in openKeychain app.")
                },
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = MainTheme.spacings.quadruple,
                    horizontal = MainTheme.spacings.default,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationClippedCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Vestibulum tempor sed massa eget fermentum. Vivamus ut vitae aliquam e augue. " +
                    "Sed nec tincidunt arcu",
                supportingText = "scelerisque fermentum. In lobortis pellentesque aliquet. Curabitur quam " +
                    "felis, sodales in leo ac, sodales rutrum quam. Quisque et odio id ex varius porta. " +
                    "Vestibulum tortor nibh, porta venenatis velit",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = MainTheme.spacings.quadruple,
                    horizontal = MainTheme.spacings.default,
                ),
                behaviour = BannerInlineNotificationCardBehaviour.Clipped,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun BannerInlineNotificationExpandedCardTextPreview() {
    PreviewWithThemesLightDark {
        Surface(modifier = Modifier.padding(MainTheme.spacings.triple)) {
            BannerInlineNotificationCard(
                icon = { Icon(imageVector = Icons.Outlined.Report) },
                title = "Vestibulum tempor sed massa eget fermentum. Vivamus ut vitae aliquam e augue. " +
                    "Sed nec tincidunt arcu",
                supportingText = "scelerisque fermentum. In lobortis pellentesque aliquet. Curabitur quam " +
                    "felis, sodales in leo ac, sodales rutrum quam. Quisque et odio id ex varius porta. " +
                    "Vestibulum tortor nibh, porta venenatis velit",
                actions = {
                    NotificationActionButton(text = "Support article", onClick = {}, isExternalLink = true)
                    NotificationActionButton(text = "Disable encryption", onClick = {})
                },
                modifier = Modifier.padding(
                    vertical = MainTheme.spacings.quadruple,
                    horizontal = MainTheme.spacings.default,
                ),
                behaviour = BannerInlineNotificationCardBehaviour.Expanded,
            )
        }
    }
}
