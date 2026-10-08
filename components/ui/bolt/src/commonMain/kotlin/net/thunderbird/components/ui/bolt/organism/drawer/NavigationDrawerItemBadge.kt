package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.text.TextLabelLarge
import net.thunderbird.components.ui.bolt.theme.BoltTheme

/**
 * A badge for a navigation drawer item with an optional icon.
 *
 * @param label The label to display.
 * @param modifier The modifier to apply.
 * @param imageVector The image vector to display (optional).
 */
@Composable
fun NavigationDrawerItemBadge(
    label: String,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextLabelLarge(
            text = label,
        )
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                modifier = Modifier.size(BoltTheme.sizes.iconSmall)
                    .padding(start = BoltTheme.spacings.quarter),
            )
        }
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemBadgePreview() {
    PreviewWithThemes {
        NavigationDrawerItemBadge(
            label = "99+",
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemBadgeWithIconPreview() {
    PreviewWithThemes {
        NavigationDrawerItemBadge(
            label = "99+",
            imageVector = Icons.Outlined.Info,
        )
    }
}
