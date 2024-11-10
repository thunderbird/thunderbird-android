package app.k9mail.feature.navigation.drawer.ui.folder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItemBadge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.ui.common.labelForCount

@Composable
internal fun FolderListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    showUnReadCount: Boolean,
    modifier: Modifier = Modifier,
) {
    if (showStarredCount) {
        FolderCountAndStarredBadge(
            unreadCount = unreadCount,
            starredCount = starredCount,
            showUnReadCount = showUnReadCount,
            modifier = modifier,
        )
    } else {
        FolderCountBadge(
            unreadCount = unreadCount,
            showUnReadCount = showUnReadCount,
            modifier = modifier,
        )
    }
}

@Composable
private fun FolderCountBadge(
    unreadCount: Int,
    showUnReadCount: Boolean,
    modifier: Modifier = Modifier,
) {
    if (unreadCount > 0 && showUnReadCount) {
        val resources = LocalContext.current.resources

        NavigationDrawerItemBadge(
            label = labelForCount(
                count = unreadCount,
                resources = resources,
            ),
            modifier = modifier,
        )
    }
}

@Composable
private fun FolderCountAndStarredBadge(
    unreadCount: Int,
    starredCount: Int,
    showUnReadCount: Boolean,
    modifier: Modifier = Modifier,
) {
    if (unreadCount > 0 || starredCount > 0) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            val resources = LocalContext.current.resources

            if (unreadCount > 0 && showUnReadCount) {
                NavigationDrawerItemBadge(
                    label = labelForCount(
                        count = unreadCount,
                        resources = resources,
                    ),
                    imageVector = Icons.Filled.Dot,
                )
            }

            if (starredCount > 0) {
                NavigationDrawerItemBadge(
                    label = labelForCount(
                        count = starredCount,
                        resources = resources,
                    ),
                    imageVector = Icons.Filled.Star,
                )
            }
        }
    }
}
