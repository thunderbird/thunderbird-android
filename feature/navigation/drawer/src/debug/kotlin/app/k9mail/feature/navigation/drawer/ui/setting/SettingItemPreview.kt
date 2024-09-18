package app.k9mail.feature.navigation.drawer.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
fun SettingItemPreview() {
    PreviewWithThemes {
        SettingItem(
            label = "Setting",
            onClick = {},
        )
    }
}
