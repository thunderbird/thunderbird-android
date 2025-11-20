package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItemBadge
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun FolderListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    FolderCountAndStarredBadge(
        unreadCount = unreadCount,
        starredCount = starredCount,
        showStarredCount = showStarredCount,
        modifier = modifier,
    )
}

@Composable
private fun FolderCountAndStarredBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        val resources = LocalResources.current

        if (unreadCount > 0) {
            NavigationDrawerItemBadge(
                label = labelForCount(
                    count = unreadCount,
                    resources = resources,
                ),
                imageVector = if (showStarredCount) Icons.Filled.Dot else null,
            )
        }

        if (showStarredCount && starredCount > 0) {
            Spacer(modifier = Modifier.width(MainTheme.spacings.half))
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
