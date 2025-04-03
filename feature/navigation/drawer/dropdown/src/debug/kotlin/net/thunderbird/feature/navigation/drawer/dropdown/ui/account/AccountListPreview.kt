package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountListPreview() {
    PreviewWithTheme {
        AccountList(
            accounts = persistentListOf(
                DISPLAY_ACCOUNT,
            ),
            selectedAccount = null,
            onAccountClick = { },
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountListWithSelectedPreview() {
    PreviewWithTheme {
        AccountList(
            accounts = persistentListOf(
                DISPLAY_ACCOUNT,
            ),
            selectedAccount = DISPLAY_ACCOUNT,
            onAccountClick = { },
            showStarredCount = false,
        )
    }
}
