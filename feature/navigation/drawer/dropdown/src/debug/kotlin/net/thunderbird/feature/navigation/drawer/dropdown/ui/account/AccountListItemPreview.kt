package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountListItemPreview() {
    PreviewWithThemes {
        AccountListItem(
            account = DISPLAY_ACCOUNT,
            onClick = { },
            selected = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountListItemSelectedPreview() {
    PreviewWithThemes {
        AccountListItem(
            account = DISPLAY_ACCOUNT,
            onClick = { },
            selected = true,
        )
    }
}
