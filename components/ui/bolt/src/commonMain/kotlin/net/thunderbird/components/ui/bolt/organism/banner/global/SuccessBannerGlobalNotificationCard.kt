package net.thunderbird.components.ui.bolt.organism.banner.global

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.organism.banner.BannerNotificationCardDefaults
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons

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
