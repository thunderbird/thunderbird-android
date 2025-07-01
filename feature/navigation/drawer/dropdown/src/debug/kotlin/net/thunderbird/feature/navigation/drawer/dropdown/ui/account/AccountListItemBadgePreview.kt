package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun AccountListItemBadgePreview() {
    PreviewWithThemes {
        AccountListItemBadge(
            unreadCount = 5,
            starredCount = 3,
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountListItemBadgeWithMaxUnreadPreview() {
    PreviewWithThemes {
        AccountListItemBadge(
            unreadCount = 999,
            starredCount = 0,
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountListItemBadgeWithStarsPreview() {
    PreviewWithThemes {
        AccountListItemBadge(
            unreadCount = 5,
            starredCount = 3,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun AccountListItemBadgeWithStarsAndMaxCountPreview() {
    PreviewWithThemes {
        AccountListItemBadge(
            unreadCount = 5,
            starredCount = 999,
            showStarredCount = true,
        )
    }
}
