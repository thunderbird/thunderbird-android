package net.thunderbird.components.ui.catalog.ui.page.molecule.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextLabelSmall
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import kotlin.random.Random
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.Icons
import net.thunderbird.components.ui.bolt.atom.tab.TabPrimary
import net.thunderbird.components.ui.bolt.atom.tab.TabSecondary
import net.thunderbird.components.ui.bolt.molecule.tab.TabRowPrimary
import net.thunderbird.components.ui.bolt.molecule.tab.TabRowSecondary
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.tabRowItems() {
    primaryTabRow()
    secondaryTabRow()
}

@Suppress("LongMethod")
private fun LazyGridScope.primaryTabRow() {
    sectionHeaderItem("Primary Tab Row")
    sectionSubtitleItem("Default")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowPrimary(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
    sectionSubtitleItem("Indicator changes")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowPrimary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            indicatorColor = BoltTheme.colors.tertiary,
            indicatorWidth = BoltTheme.sizes.medium,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
    sectionSubtitleItem("Custom Edge Padding")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5") }
        TabRowPrimary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = BoltTheme.spacings.zero,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabPrimary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
    sectionSubtitleItem("Tab Row with Icons")
    fullSpanItem {
        TabRowPrimaryWithIconAndMaybeBadge(showBadge = false)
    }
    sectionSubtitleItem("Tab Row with Icons and Badges")
    fullSpanItem {
        TabRowPrimaryWithIconAndMaybeBadge(showBadge = true)
    }
}

@Composable
private fun TabRowPrimaryWithIconAndMaybeBadge(showBadge: Boolean) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember {
        listOf(
            Icons.Outlined.Inbox,
            Icons.Outlined.Archive,
            Icons.Outlined.Outbox,
            Icons.Outlined.Send,
            Icons.Outlined.Delete,
            Icons.Outlined.Folder,
            Icons.Outlined.Settings,
        ).mapIndexed { index, icon ->
            object {
                val title = "Tab $index"
                val icon = icon
                val badge = if (Random.nextBoolean()) Random.nextInt(from = 1, until = 99) else 0
            }
        }
    }
    TabRowPrimary(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        edgePadding = BoltTheme.spacings.zero,
    ) {
        tabs.forEachIndexed { index, tab ->
            TabPrimary(
                selected = selectedTab == index,
                title = {
                    if (selectedTab == index) {
                        TextTitleMedium(text = tab.title)
                    } else {
                        TextBodyLarge(text = tab.title)
                    }
                },
                onClick = { selectedTab = index },
                icon = { Icon(imageVector = tab.icon) },
                badge = if (showBadge && tab.badge > 0) {
                    { TextLabelSmall(tab.badge.toString()) }
                } else {
                    null
                },
            )
        }
    }
}

@Suppress("LongMethod")
private fun LazyGridScope.secondaryTabRow() {
    sectionHeaderItem("Secondary Tab Row")
    sectionSubtitleItem("Default")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowSecondary(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                TabSecondary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
    sectionSubtitleItem("Indicator changes")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3") }
        TabRowSecondary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            indicatorColor = BoltTheme.colors.tertiary,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabSecondary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
    sectionSubtitleItem("Custom Edge Padding")
    fullSpanItem {
        var selectedTab by remember { mutableIntStateOf(0) }
        val tabs = remember { listOf("Tab 1", "Tab 2", "Tab 3", "Tab 4", "Tab 5") }
        TabRowSecondary(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth(),
            edgePadding = BoltTheme.spacings.zero,
        ) {
            tabs.forEachIndexed { index, tab ->
                TabSecondary(
                    selected = selectedTab == index,
                    title = {
                        if (selectedTab == index) {
                            TextTitleMedium(text = tab)
                        } else {
                            TextBodyLarge(text = tab)
                        }
                    },
                    onClick = { selectedTab = index },
                )
            }
        }
    }
}
