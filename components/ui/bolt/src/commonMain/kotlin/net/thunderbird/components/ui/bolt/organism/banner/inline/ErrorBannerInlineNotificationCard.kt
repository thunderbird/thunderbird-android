package net.thunderbird.components.ui.bolt.organism.banner.inline

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.molecule.notification.NotificationActionButton
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults

/**
 * Displays an error banner inline notification card.
 *
 * @param title The main text or heading of the notification.
 * @param supportingText Additional details or context for the notification.
 * @param actions A composable lambda that defines the actions available in the notification,
 *  typically buttons, laid out in a [RowScope].
 * @param modifier Optional [Modifier] to be applied to the composable.
 * @param behaviour Optional [BannerInlineNotificationCardBehaviour] to customize the appearance
 *  and behavior of the notification card. Defaults to [BannerNotificationCardDefaults.bannerInlineBehaviour].
 */
@Composable
fun ErrorBannerInlineNotificationCard(
    title: CharSequence,
    supportingText: CharSequence,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    behaviour: BannerInlineNotificationCardBehaviour = BannerNotificationCardDefaults.bannerInlineBehaviour,
    onSupportingTextOverflow: (hasVisualOverflow: Boolean) -> Unit = {},
) {
    BannerInlineNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.Report) },
        title = title,
        supportingText = supportingText,
        actions = actions,
        modifier = modifier,
        behaviour = behaviour,
        colors = BannerNotificationCardDefaults.errorCardColors(),
        border = BannerNotificationCardDefaults.errorCardBorder(),
        onSupportingTextOverflow = onSupportingTextOverflow,
    )
}

@PreviewLightDark
@Composable
private fun ErrorBannerInlineNotificationCardPreviewPreview() {
    PreviewWithThemesLightDark {
        ErrorBannerInlineNotificationCard(
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
private fun ErrorBannerInlineNotificationCardLongTextClippedPreviewPreview() {
    val title = remember { LoremIpsum(words = 20).values.joinToString(" ") }
    val supportingText = remember { LoremIpsum(words = 60).values.joinToString(" ") }
    PreviewWithThemesLightDark {
        ErrorBannerInlineNotificationCard(
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
private fun ErrorBannerInlineNotificationCardLongTextExpandedPreviewPreview() {
    val title = remember { LoremIpsum(words = 20).values.joinToString(" ") }
    val supportingText = remember { LoremIpsum(words = 60).values.joinToString(" ") }
    PreviewWithThemesLightDark {
        ErrorBannerInlineNotificationCard(
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
