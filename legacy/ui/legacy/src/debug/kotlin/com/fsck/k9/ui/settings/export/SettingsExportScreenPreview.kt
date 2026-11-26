package com.fsck.k9.ui.settings.export

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import net.thunderbird.components.ui.bolt.PreviewWithThemesLightDark

@Composable
@PreviewLightDark
internal fun SettingsExportScreenPreview() {
    PreviewWithThemesLightDark {
        SettingsExportScreen(
            onShareClick = {},
            onExportClick = {},
            onSettingsListItemSelect = { _, _ -> },
            state = SettingsExportContract.State(
                isSettingsListEnabled = true,
                exportButtonState = ButtonState.ENABLED,
                isShareButtonVisible = false,
                isProgressVisible = false,
                statusText = StatusText.HIDDEN,
                settingsList = listOf(
                    SettingsListItem.GeneralSettings(),
                    SettingsListItem.Account(1, "Personal", "personal@example.com"),
                    SettingsListItem.Account(2, "Work", "work@example.com"),
                    SettingsListItem.Account(3, "Family", "family@example.com"),
                    SettingsListItem.Account(4, "Business", "business@example.com"),
                    SettingsListItem.Account(5, "Support", "support@example.com"),
                    SettingsListItem.Account(6, "Development", "development@example.com"),
                    SettingsListItem.Account(7, "Testing", "testing@example.com"),
                    SettingsListItem.Account(8, "Finance", "finance@example.com"),
                    SettingsListItem.Account(9, "Marketing", "marketing@example.com"),
                    SettingsListItem.Account(10, "Sales", "sales@example.com"),
                    SettingsListItem.Account(11, "Personal 2", "personal2@example.com"),
                    SettingsListItem.Account(12, "Work 2", "work2@example.com"),
                    SettingsListItem.Account(13, "Archive", "archive@example.com"),
                    SettingsListItem.Account(14, "Notifications", "notifications@example.com"),
                    SettingsListItem.Account(15, "Projects", "projects@example.com"),
                    SettingsListItem.Account(16, "Research", "research@example.com"),
                    SettingsListItem.Account(17, "Operations", "operations@example.com"),
                    SettingsListItem.Account(18, "Administration", "admin@example.com"),
                    SettingsListItem.Account(19, "Backup", "backup@example.com"),
                    SettingsListItem.Account(20, "Secondary", "secondary@example.com"),
                ),
            ),
        )
    }
}
