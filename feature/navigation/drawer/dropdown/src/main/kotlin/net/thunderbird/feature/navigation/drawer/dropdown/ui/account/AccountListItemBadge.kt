package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.drawer.NavigationDrawerItemBadge
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.navigation.drawer.dropdown.ui.common.labelForCount

@Composable
internal fun AccountListItemBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    AccountCountAndStarredBadge(
        unreadCount = unreadCount,
        starredCount = starredCount,
        showStarredCount = showStarredCount,
        modifier = modifier,
    )
}

@Composable
private fun AccountCountAndStarredBadge(
    unreadCount: Int,
    starredCount: Int,
    showStarredCount: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
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
            Spacer(modifier = Modifier.Companion.width(MainTheme.spacings.half))
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
