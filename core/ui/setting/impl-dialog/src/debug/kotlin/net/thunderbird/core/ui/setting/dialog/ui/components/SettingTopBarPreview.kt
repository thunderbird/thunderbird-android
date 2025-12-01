package net.thunderbird.core.ui.setting.dialog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun SettingTopBarPreview() {
    PreviewWithThemes {
        SettingTopBar(
            title = "Title",
            subtitle = null,
            onBack = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SettingTopBarWithSubtitlePreview() {
    PreviewWithThemes {
        SettingTopBar(
            title = "Title",
            subtitle = "Subtitle",
            onBack = {},
        )
    }
}
