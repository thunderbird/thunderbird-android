package app.k9mail.ui.catalog.ui.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.designsystem.organism.SubtitleTopAppBar
import app.k9mail.core.ui.compose.designsystem.organism.SubtitleTopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.organism.SubtitleTopAppBarWithMenuButton
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBar
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithBackButton
import app.k9mail.core.ui.compose.designsystem.organism.TopAppBarWithMenuButton
import app.k9mail.ui.catalog.ui.common.list.ItemOutlinedView
import app.k9mail.ui.catalog.ui.common.list.fullSpanItem
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem
import app.k9mail.ui.catalog.ui.common.list.sectionSubtitleItem

fun LazyGridScope.appBarItems() {
    topAppBarItems()
    subtitleTopAppBarItems()
}

private fun LazyGridScope.topAppBarItems() {
    sectionHeaderItem(text = "TopAppBar")
    sectionSubtitleItem(text = "With menu icon")
    fullSpanItem {
        ItemOutlinedView {
            TopAppBar(
                title = "Title",
                actions = {
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Info,
                    )
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Check,
                    )
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Visibility,
                    )
                },
            )
        }
    }
    sectionSubtitleItem(text = "With back menu icon")
    fullSpanItem {
        ItemOutlinedView {
            TopAppBarWithMenuButton(
                title = "Title",
                onMenuClick = {},
            )
        }
    }
    sectionSubtitleItem(text = "With back icon")
    fullSpanItem {
        ItemOutlinedView {
            TopAppBarWithBackButton(
                title = "Title",
                onBackClick = {},
            )
        }
    }
}

private fun LazyGridScope.subtitleTopAppBarItems() {
    sectionHeaderItem(text = "SubtitleTopAppBar")
    sectionSubtitleItem(text = "With menu icon")
    fullSpanItem {
        ItemOutlinedView {
            SubtitleTopAppBar(
                title = "Title",
                subtitle = "Subtitle",
                actions = {
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Info,
                    )
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Check,
                    )
                    ButtonIcon(
                        onClick = {},
                        imageVector = Icons.Outlined.Visibility,
                    )
                },
            )
        }
    }
    sectionSubtitleItem(text = "With back menu icon")
    fullSpanItem {
        ItemOutlinedView {
            SubtitleTopAppBarWithMenuButton(
                title = "Title",
                subtitle = "Subtitle",
                onMenuClick = {},
            )
        }
    }
    sectionSubtitleItem(text = "With back icon")
    fullSpanItem {
        ItemOutlinedView {
            SubtitleTopAppBarWithBackButton(
                title = "Title",
                subtitle = "Subtitle",
                onBackClick = {},
            )
        }
    }
}
