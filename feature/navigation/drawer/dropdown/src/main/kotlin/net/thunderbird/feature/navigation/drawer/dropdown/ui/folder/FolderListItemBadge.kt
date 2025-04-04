package net.thunderbird.feature.navigation.drawer.dropdown.ui.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    FolderCountAndStarredBadge(
        unreadCount = unreadCount,
        starredCount = starredCount,
        showStarredCount = showStarredCount,
        onClick = { expandableState?.value = !expandableState.value },
        isExpanded = expandableState?.value,
        modifier = modifier,
    )
}

@Composable
private fun FolderCountAndStarredBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpanded: Boolean? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val resources = LocalContext.current.resources

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

        if (isExpanded != null) {
            Box(
                modifier = Modifier
                    .height(MainTheme.sizes.iconAvatar)
                    .padding(start = MainTheme.spacings.quarter)
                    .clip(CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    modifier = Modifier.size(MainTheme.sizes.iconLarge),
                )
            }
        }
    }
}
