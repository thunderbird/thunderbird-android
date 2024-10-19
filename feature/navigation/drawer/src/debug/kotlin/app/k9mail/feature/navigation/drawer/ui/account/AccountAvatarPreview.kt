package app.k9mail.feature.navigation.drawer.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import app.k9mail.feature.navigation.drawer.ui.FakeData.DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountAvatarPreview() {
    PreviewWithThemes {
        AccountAvatar(
            account = DISPLAY_ACCOUNT,
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAvatarWithUnreadCountPreview() {
    PreviewWithThemes {
        AccountAvatar(
            account = DISPLAY_ACCOUNT.copy(
                unreadMessageCount = 12,
            ),
            onClick = {},
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAvatarWithUnreadCountMaxedPreview() {
    PreviewWithThemes {
        AccountAvatar(
            account = DISPLAY_ACCOUNT.copy(
                unreadMessageCount = 100,
            ),
            onClick = {},
        )
    }
}
