package net.thunderbird.feature.navigation.drawer.siderail.ui.setting

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme

@Composable
@Preview(showBackground = true)
internal fun SideRailSettingListPreview() {
    PreviewWithTheme {
        SideRailSettingList(
            onAccountSelectorClick = {},
            onManageFoldersClick = {},
            showAccountSelector = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailSettingListShowAccountSelectorPreview() {
    PreviewWithTheme {
        SideRailSettingList(
            onAccountSelectorClick = {},
            onManageFoldersClick = {},
            showAccountSelector = true,
        )
    }
}
