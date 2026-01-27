package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.MAIL_DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountViewPreview() {
    PreviewWithThemes {
        AccountView(
            account = MAIL_DISPLAY_ACCOUNT,
            onClick = {},
            onAvatarClick = {},
            showAccountSelection = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountViewWithoutAccountPreview() {
    PreviewWithThemes {
        AccountView(
            account = MAIL_DISPLAY_ACCOUNT,
            onClick = {},
            onAvatarClick = {},
            showAccountSelection = false,
        )
    }
}
