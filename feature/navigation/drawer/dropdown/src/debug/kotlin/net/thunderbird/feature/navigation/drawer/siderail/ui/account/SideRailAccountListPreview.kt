package net.thunderbird.feature.navigation.drawer.siderail.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountListPreview() {
    PreviewWithTheme {
        SideRailAccountList(
            accounts = persistentListOf(
                DISPLAY_ACCOUNT,
            ),
            selectedAccount = null,
            onAccountClick = { },
            onSettingsClick = { },
            onSyncAllAccountsClick = { },
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun SideRailAccountListWithSelectedPreview() {
    PreviewWithTheme {
        SideRailAccountList(
            accounts = persistentListOf(
                DISPLAY_ACCOUNT,
            ),
            selectedAccount = DISPLAY_ACCOUNT,
            onAccountClick = { },
            onSettingsClick = { },
            onSyncAllAccountsClick = { },
        )
    }
}
