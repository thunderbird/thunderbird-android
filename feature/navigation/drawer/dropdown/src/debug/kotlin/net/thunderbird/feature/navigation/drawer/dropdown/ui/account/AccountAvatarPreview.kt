package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes
import net.thunderbird.feature.navigation.drawer.dropdown.ui.FakeData.MAIL_DISPLAY_ACCOUNT

@Composable
@Preview(showBackground = true)
internal fun AccountAvatarPreview() {
    PreviewWithThemes {
        AccountAvatar(
            account = MAIL_DISPLAY_ACCOUNT,
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
            account = MAIL_DISPLAY_ACCOUNT.copy(
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
            account = MAIL_DISPLAY_ACCOUNT.copy(
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
            account = MAIL_DISPLAY_ACCOUNT.copy(
                color = 0xFFFF0000.toInt(),
            ),
            onClick = {},
            selected = true,
        )
    }
}
