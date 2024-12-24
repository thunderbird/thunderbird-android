package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItemBadge
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun FolderListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
    expandableState: MutableState<Boolean>? = null,
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

    if (expandableState !== null) {
        FolderExpandableBadge(
            isExpanded = expandableState.value,
            onClick = { expandableState.value = !expandableState.value }
        )
    }
}

@Composable
private fun FolderExpandableBadge(
    isExpanded: Boolean = false,
    onClick: () -> Unit
) {
    Icon(
        imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
        modifier = Modifier
            .size(MainTheme.sizes.iconLarge)
            .padding(end = MainTheme.spacings.quarter)
            .clickable(onClick = onClick)
    )
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
