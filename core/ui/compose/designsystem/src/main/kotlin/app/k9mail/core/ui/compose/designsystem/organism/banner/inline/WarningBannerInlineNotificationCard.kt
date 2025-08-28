package app.k9mail.core.ui.compose.designsystem.organism.banner.inline

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.icon.outlined.Warning
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults

/**
 * Displays a warning banner inline notification card.
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
fun WarningBannerInlineNotificationCard(
    title: CharSequence,
    supportingText: CharSequence,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    behaviour: BannerInlineNotificationCardBehaviour = BannerNotificationCardDefaults.bannerInlineBehaviour,
) {
    BannerInlineNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.Warning) },
        title = title,
        supportingText = supportingText,
        actions = actions,
        modifier = modifier,
        behaviour = behaviour,
        colors = BannerNotificationCardDefaults.warningCardColors(),
        border = BannerNotificationCardDefaults.warningCardBorder(),
    )
}
