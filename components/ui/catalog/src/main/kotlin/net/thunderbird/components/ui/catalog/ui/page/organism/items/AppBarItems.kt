package net.thunderbird.components.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import net.thunderbird.components.ui.bolt.atom.button.ButtonIcon
import net.thunderbird.components.ui.bolt.organism.SubtitleTopAppBar
import net.thunderbird.components.ui.bolt.organism.SubtitleTopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.organism.SubtitleTopAppBarWithMenuButton
import net.thunderbird.components.ui.bolt.organism.TopAppBar
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithBackButton
import net.thunderbird.components.ui.bolt.organism.TopAppBarWithMenuButton
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.catalog.ui.page.common.list.ItemOutlinedView
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

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

@Suppress("LongMethod")
private fun LazyGridScope.subtitleTopAppBarItems() {
    sectionHeaderItem(text = "SubtitleTopAppBar")
    sectionSubtitleItem(text = "With menu icon")
    fullSpanItem {
        ItemOutlinedView {
            SubtitleTopAppBar(
                title = "Title",
                subtitle = "Subtitle",
                actions = {
                    DemoActionButton(
                        imageVector = Icons.Outlined.Info,
                    )
                    DemoActionButton(
                        imageVector = Icons.Outlined.Check,
                    )
                    DemoActionButton(
                        imageVector = Icons.Outlined.Visibility,
                    )
                },
            )
        }
    }
    sectionSubtitleItem(text = "With long subtitle")
    fullSpanItem {
        ItemOutlinedView {
            SubtitleTopAppBar(
                title = "Title",
                subtitle = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                actions = {
                    DemoActionButton(
                        imageVector = Icons.Outlined.Info,
                    )
                    DemoActionButton(
                        imageVector = Icons.Outlined.Check,
                    )
                    DemoActionButton(
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

/**
 * Demo action button that does nothing on click.
 */
@Composable
private fun DemoActionButton(
    imageVector: ImageVector,
) {
    ButtonIcon(
        onClick = {},
        imageVector = imageVector,
    )
}
