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
            selected = false,
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
            selected = false,
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
            selected = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountAvatarSelectedPreview() {
    PreviewWithThemes {
        AccountAvatar(
            account = DISPLAY_ACCOUNT.copy(
                color = 0xFFFF0000.toInt(),
            ),
            onClick = {},
            selected = true,
        )
    }
}
