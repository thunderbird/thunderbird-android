package app.k9mail.feature.navigation.drawer.ui.folder

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItemBadge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.feature.navigation.drawer.R

@Composable
internal fun FolderListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    if (showStarredCount) {
        FolderCountAndStarredBadge(
            unreadCount = unreadCount,
            starredCount = starredCount,
            modifier = modifier,
        )
    } else {
        FolderCountBadge(
            unreadCount = unreadCount,
            modifier = modifier,
        )
    }
}

@Composable
private fun FolderCountBadge(
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    if (unreadCount > 0) {
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
    modifier: Modifier = Modifier,
) {
    if (unreadCount > 0 || starredCount > 0) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            val resources = LocalContext.current.resources

            if (unreadCount > 0) {
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

@Suppress("MagicNumber")
private fun labelForCount(
    count: Int,
    resources: Resources,
) = when {
    count in 1..99 -> "$count"

    count in 100..1000 -> resources.getString(
        R.string.navigation_drawer_folder_item_badge_count_greater_than_99,
    )

    count > 1000 -> resources.getString(
        R.string.navigation_drawer_folder_item_badge_count_greater_than_1_000,
    )

    else -> ""
}
