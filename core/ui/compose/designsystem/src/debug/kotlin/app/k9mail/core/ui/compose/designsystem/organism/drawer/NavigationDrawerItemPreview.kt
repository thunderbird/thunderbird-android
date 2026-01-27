package app.k9mail.core.ui.compose.designsystem.organism.drawer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelLarge
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemSelectedPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = true,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemUnselectedPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemWithIconPreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountBox,
                )
            },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun NavigationDrawerItemWithLabelBadgePreview() {
    PreviewWithThemes {
        NavigationDrawerItem(
            label = "DrawerItem",
            selected = false,
            onClick = {},
            badge = {
                TextLabelLarge(
                    text = "100+",
                )
            },
        )
    }
}
