package app.k9mail.core.ui.compose.designsystem.organism.banner.inline

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

/**
 * Displays a success banner inline notification card.
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
fun SuccessBannerInlineNotificationCard(
    title: CharSequence,
    supportingText: CharSequence,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    behaviour: BannerInlineNotificationCardBehaviour = BannerNotificationCardDefaults.bannerInlineBehaviour,
    onSupportingTextOverflow: (hasVisualOverflow: Boolean) -> Unit = {},
) {
    BannerInlineNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.CheckCircle) },
        title = title,
        supportingText = supportingText,
        actions = actions,
        modifier = modifier,
        behaviour = behaviour,
        colors = BannerNotificationCardDefaults.successCardColors(),
        border = BannerNotificationCardDefaults.successCardBorder(),
        onSupportingTextOverflow = onSupportingTextOverflow,
    )
}
