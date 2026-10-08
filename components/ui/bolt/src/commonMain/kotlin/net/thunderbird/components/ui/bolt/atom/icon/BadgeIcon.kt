package net.thunderbird.components.ui.bolt.atom.icon

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon as Material3Icon
import androidx.compose.material3.LocalContentColor as Material3LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * Badge icon composable to display an badge icon.
 *
 * @param imageVector The icon to be displayed as a badge.
 * @param modifier The modifier to be applied to the badge.
 * @param contentDescription The content description for accessibility.
 * @param tint The tint color for the badge. Defaults to the current content color.
 */
@Composable
fun BadgeIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color? = null,
) {
    Material3Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(BoltTheme.sizes.badge),
        tint = tint ?: Material3LocalContentColor.current,
    )
}

@Composable
@PreviewLightDark
internal fun BadgeIconPreview() {
    BadgeIcon(
        imageVector = BadgeIcons.Filled.NewMail,
        contentDescription = "NewMail Badge",
    )
}

@Composable
@PreviewLightDark
internal fun BadgeIconWithTintPreview() {
    BadgeIcon(
        imageVector = BadgeIcons.Filled.NewMail,
        tint = Color.Red,
        contentDescription = "NewMail Badge",
    )
}
