package net.thunderbird.core.ui.setting.dialog.ui

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.common.annotation.PreviewDevicesWithBackground
import net.thunderbird.core.ui.setting.dialog.ui.fake.FakeSettingData

@Composable
@PreviewDevicesWithBackground
fun SettingVViewWithDialogPreview() {
    PreviewWithTheme {
        DialogSettingView(
            title = "Title",
            subtitle = "Subtitle",
            settings = FakeSettingData.settings,
            onSettingValueChange = {},
            onBack = {},
        )
    }
}
