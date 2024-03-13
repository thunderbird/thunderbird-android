package app.k9mail.ui.catalog.ui.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.appBarItems() {
    sectionHeaderItem(text = "TopAppBar")
    sectionSubtitleItem(text = "With menu icon")
    item {
        ItemOutlined {
            TopAppBarItem(
                title = "Title",
                navIcon = Icons.Outlined.menu,
                actionIcon = Icons.Filled.user,
            )
        }
    }
    sectionSubtitleItem(text = "With back icon")
    item {
        ItemOutlined {
            TopAppBarItem(
                title = "Title",
                navIcon = Icons.Outlined.arrowBack,
                actionIcon = Icons.Filled.inbox,
            )
        }
    }
    sectionSubtitleItem(text = "With subtitle")
    item {
        ItemOutlined {
            TopAppBarItem(
                title = "Title",
                subtitle = "Subtitle",
                navIcon = Icons.Outlined.menu,
                actionIcon = Icons.Filled.user,
            )
        }
    }
    sectionSubtitleItem(text = "With subtitle and no nav icon")
    item {
        ItemOutlined {
            TopAppBarItem(
                title = "Title",
                subtitle = "Subtitle",
                navIcon = null,
                actionIcon = Icons.Filled.user,
            )
        }
    }
}

@Composable
fun TopAppBarItem(
    title: String,
    actionIcon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navIcon: ImageVector? = null,
) {
    TopAppBar(
        title = title,
        subtitle = subtitle,
        navigationIcon = navIcon?.let {
            {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = navIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = null,
                )
            }
        },
        modifier = modifier,
    )
}
