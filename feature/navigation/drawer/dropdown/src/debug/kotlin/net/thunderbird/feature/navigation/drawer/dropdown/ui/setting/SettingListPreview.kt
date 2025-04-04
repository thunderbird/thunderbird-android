package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun SettingListPreview() {
    PreviewWithTheme {
        SettingList(
            onAccountSelectorClick = {},
            onManageFoldersClick = {},
            showAccountSelector = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SettingListShowAccountSelectorPreview() {
    PreviewWithTheme {
        SettingList(
            onAccountSelectorClick = {},
            onManageFoldersClick = {},
            showAccountSelector = true,
        )
    }
}
