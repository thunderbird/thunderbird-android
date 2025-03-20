package net.thunderbird.core.ui.compose.preference.ui

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf

@Composable
@PreviewDevicesWithBackground
fun PreferenceViewPreview() {
    PreviewWithTheme {
        PreferenceView(
            title = "Title",
            subtitle = "Subtitle",
            preferences = persistentListOf(),
            onPreferenceChange = {},
            onBack = {},
        )
    }
}
