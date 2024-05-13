package app.k9mail.ui.catalog.ui.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithMenuButton
import app.k9mail.ui.catalog.ui.common.list.ItemOutlined
import app.k9mail.ui.catalog.ui.common.list.fullSpanItem
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.appBarItems() {
    sectionHeaderItem(text = "TopAppBar")
    sectionSubtitleItem(text = "With menu icon")
    fullSpanItem {
        ItemOutlined {
            TopAppBarItem(
                title = "Title",
                actionIcon = Icons.Outlined.Info,
            )
        }
    }
    sectionSubtitleItem(text = "With back menu icon")
    fullSpanItem {
        ItemOutlined {
            TopAppBarWithMenuButton(
                title = "Title",
                onMenuClick = {},
            )
        }
    }
    sectionSubtitleItem(text = "With back icon")
    fullSpanItem {
        ItemOutlined {
            TopAppBarWithBackButton(
                title = "Title",
                onBackClick = {},
            )
        }
    }
}

@Composable
fun TopAppBarItem(
    title: String,
    actionIcon: ImageVector,
    modifier: Modifier = Modifier,
    navIcon: ImageVector? = null,
) {
    TopAppBar(
        title = title,
        navigationIcon = {
            navIcon?.let {
                ButtonIcon(
                    onClick = {},
                    imageVector = Icons.Outlined.Menu,
                )
            }
        },
        actions = {
            ButtonIcon(
                onClick = {},
                imageVector = actionIcon,
            )
        },
        modifier = modifier,
    )
}
