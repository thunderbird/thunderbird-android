package app.k9mail.core.ui.compose.designsystem.organism.banner.global

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.atom.icon.outlined.Warning
import app.k9mail.core.ui.compose.designsystem.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

/**
 * Displays a warning banner global notification card.
 *
 * @param text The text to display in the notification card.
 * @param action The composable function to display as the action button.
 * @param modifier The modifier to apply to this layout node.
 */
@Composable
fun WarningBannerGlobalNotificationCard(
    text: CharSequence,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    BannerGlobalNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.Warning) },
        text = text,
        action = action,
        modifier = modifier,
        colors = BannerNotificationCardDefaults.warningCardColors(),
    )
}
