package net.thunderbird.components.ui.bolt.organism.drawer

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icons

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
