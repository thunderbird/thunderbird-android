package net.thunderbird.core.ui.setting.dialog.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
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
