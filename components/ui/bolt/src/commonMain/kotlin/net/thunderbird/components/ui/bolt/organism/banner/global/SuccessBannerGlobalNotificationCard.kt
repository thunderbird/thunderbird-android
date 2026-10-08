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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * Displays a success banner global notification card.
 *
 * @param text The text to display in the notification card.
 * @param action The composable function to display as the action button.
 * @param modifier The modifier to apply to this layout node.
 */
@Composable
fun SuccessBannerGlobalNotificationCard(
    text: CharSequence,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    BannerGlobalNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.CheckCircle) },
        text = text,
        action = action,
        modifier = modifier,
        colors = BannerNotificationCardDefaults.successCardColors(),
    )
}

@PreviewLightDark
@Composable
private fun SuccessBannerGlobalNotificationCardStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuccessBannerGlobalNotificationCard(
                text = "What an awesome notification, isn't it?",
                action = {
                    NotificationActionButton(
                        text = "Action",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SuccessBannerGlobalNotificationCardNoActionPreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuccessBannerGlobalNotificationCard(
                text = "What an awesome notification, isn't it?",
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SuccessBannerGlobalNotificationCardLongTextPreview(
    @PreviewParameter(LoremIpsum::class) text: String,
) {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuccessBannerGlobalNotificationCard(
                text = text,
                action = {
                    NotificationActionButton(
                        text = "Retry",
                        onClick = {},
                    )
                },
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SuccessBannerGlobalNotificationCardLongNoActionTextPreview(
    @PreviewParameter(LoremIpsum::class) text: String,
) {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuccessBannerGlobalNotificationCard(
                text = text,
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SuccessBannerGlobalNotificationCardAnnotatedStringTitlePreview() {
    PreviewWithThemesLightDark {
        Surface(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SuccessBannerGlobalNotificationCard(
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
                modifier = Modifier.padding(top = BoltTheme.spacings.quadruple),
            )
        }
    }
}
