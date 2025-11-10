package net.thunderbird.feature.navigation.drawer.siderail.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Composable
@Preview(showBackground = true)
internal fun SideRailSettingItemPreview() {
    PreviewWithThemes {
        SideRailSettingItem(
            icon = Icons.Outlined.Settings,
            label = "Setting",
            onClick = {},
        )
    }
}
