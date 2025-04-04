package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.k9mail.core.ui.compose.designsystem.PreviewWithThemes

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgePreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 99,
            starredCount = 0,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWithStarredCountPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 99,
            starredCount = 1,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWithZeroUnreadCountPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 0,
            starredCount = 1,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWithZeroStarredCountPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 99,
            starredCount = 0,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWithZeroCountsPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 0,
            starredCount = 0,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWithoutStarredCountPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 99,
            starredCount = 1,
            showStarredCount = false,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWith100CountsPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 100,
            starredCount = 100,
            showStarredCount = true,
        )
    }
}

@Composable
@Preview(showBackground = true)
internal fun FolderListItemBadgeWith1000CountsPreview() {
    PreviewWithThemes {
        FolderListItemBadge(
            unreadCount = 1000,
            starredCount = 1000,
            showStarredCount = true,
        )
    }
}
