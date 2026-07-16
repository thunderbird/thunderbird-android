package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.PreviewWithThemes
import net.thunderbird.components.ui.bolt.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun SettingListItemPreview() {
    PreviewWithThemes {
        SettingListItem(
            label = "Settings",
            onClick = {},
            icon = Icons.Outlined.Settings,
        )
    }
}
