package app.k9mail.core.ui.compose.designsystem.organism.banner.global

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults

/**
 * Displays an error banner global notification card.
 *
 * @param text The text to display in the notification card.
 * @param action The composable function to display as the action button.
 * @param modifier The modifier to apply to this layout node.
 */
@Composable
fun ErrorBannerGlobalNotificationCard(
    text: CharSequence,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    BannerGlobalNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.Report) },
        text = text,
        action = action,
        modifier = modifier,
        colors = BannerNotificationCardDefaults.errorCardColors(),
    )
}
