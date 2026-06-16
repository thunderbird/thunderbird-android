package net.thunderbird.components.ui.bolt.organism.banner.global

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

/**
 * Displays an info banner global notification card.
 *
 * @param text The text to display in the notification card.
 * @param action The composable function to display as the action button.
 * @param modifier The modifier to apply to this layout node.
 */
@Composable
fun InfoBannerGlobalNotificationCard(
    text: CharSequence,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    BannerGlobalNotificationCard(
        icon = { Icon(imageVector = Icons.Outlined.Info) },
        text = text,
        action = action,
        modifier = modifier,
        colors = BannerNotificationCardDefaults.infoCardColors(),
    )
}
