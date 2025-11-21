package net.thunderbird.ui.catalog.ui.page.template.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import kotlin.random.Random
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.template.pager.HorizontalTabPagerPrimary
import net.thunderbird.core.ui.compose.designsystem.template.pager.HorizontalTabPagerSecondary
import net.thunderbird.core.ui.compose.designsystem.template.pager.TabPrimaryConfig
import net.thunderbird.core.ui.compose.designsystem.template.pager.TabSecondaryConfig
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionSubtitleItem

fun LazyGridScope.horizontalPagerItems() {
    horizontalPagerPrimary()
    horizontalPagerSecondary()
}

private fun LazyGridScope.horizontalPagerPrimary() {
    sectionHeaderItem("Horizontal Pager Primary")
    sectionSubtitleItem("Default")
    fullSpanItem {
        val pages by remember {
            mutableStateOf(
                List(size = 10) {
                    object {
                        val tab = "Tab $it"
                        val page = "Content $it"
                    }
                },
            )
        }
        HorizontalTabPagerPrimary(
            initialSelected = pages.first(),
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 250.dp),
        ) {
            pages(items = pages, tabConfigBuilder = { TabPrimaryConfig(it.tab) }) {
                Page(pageContent = it.page)
            }
        }
    }
    sectionSubtitleItem("Tabs with Icons")
    fullSpanItem {
        HorizontalTabPagerItemsWithIconAndMaybeBadge(showBadge = false)
    }
    sectionSubtitleItem("Tabs with Icons and Badges")
    fullSpanItem {
        HorizontalTabPagerItemsWithIconAndMaybeBadge(showBadge = true)
    }
}

private fun LazyGridScope.horizontalPagerSecondary() {
    sectionHeaderItem("Horizontal Pager Secondary")
    fullSpanItem {
        val pages by remember {
            mutableStateOf(
                List(size = 10) {
                    object {
                        val tab = "Tab $it"
                        val page = "Content $it"
                    }
                },
            )
        }
        HorizontalTabPagerSecondary(
            initialSelected = pages.first(),
            modifier = Modifier
                .fillMaxWidth()
                .height(height = 250.dp),
        ) {
            pages(items = pages, tabConfigBuilder = { TabSecondaryConfig(it.tab) }) {
                Page(pageContent = it.page)
            }
        }
    }
}

@Composable
private fun HorizontalTabPagerItemsWithIconAndMaybeBadge(showBadge: Boolean) {
    val pages = remember {
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
                val tab = "Tab $index"
                val content = "Content $index"
                val icon = icon
                val badge = if (Random.nextBoolean()) Random.nextInt(from = 1, until = 99) else 0
            }
        }
    }
    HorizontalTabPagerPrimary(
        initialSelected = pages.first(),
        modifier = Modifier
            .fillMaxWidth()
            .height(height = 250.dp),
    ) {
        pages(
            items = pages,
            tabConfigBuilder = {
                TabPrimaryConfig(
                    title = it.tab,
                    icon = it.icon,
                    badgeCount = if (showBadge) it.badge else null,
                )
            },
        ) { page ->
            Page(pageContent = page.content)
        }
    }
}

@Composable
private fun Page(pageContent: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TextBodyLarge(text = "Content for $pageContent")
    }
}
