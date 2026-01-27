package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.TabRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.text.TextLabelSmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons
import net.thunderbird.core.ui.compose.designsystem.atom.tab.TabPrimary
import net.thunderbird.core.ui.compose.designsystem.atom.tab.TabSecondary
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.fullSpanItem
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem

fun LazyGridScope.tabItems() {
    sectionHeaderItem("Primary Tab")
    fullSpanItem {
        var selected by remember { mutableStateOf(PrimaryTabItems.TextOnly) }
        TabRow(selectedTabIndex = PrimaryTabItems.entries.indexOf(selected)) {
            PrimaryTabItems.entries.forEach { tabItem ->
                TabPrimary(
                    selected = selected == tabItem,
                    title = { TextTitleMedium(tabItem.name) },
                    onClick = { selected = tabItem },
                    icon = {
                        when (tabItem) {
                            PrimaryTabItems.TextOnly -> null
                            PrimaryTabItems.TextWithIcon, PrimaryTabItems.TextWithIconAndBadge ->
                                Icon(imageVector = requireNotNull(tabItem.icon))
                        }
                    },
                    badge = if (tabItem == PrimaryTabItems.TextWithIconAndBadge) {
                        { TextLabelSmall(text = tabItem.badgeCount.toString()) }
                    } else {
                        null
                    },
                )
            }
        }
    }
    sectionHeaderItem("Secondary Tab")
    defaultItem {
        TabRow(selectedTabIndex = 0) {
            TabSecondary(
                selected = true,
                title = { TextTitleMedium("Secondary Tab") },
                onClick = { },
            )
        }
    }
}

private enum class PrimaryTabItems(
    val text: String,
    val icon: ImageVector?,
    val badgeCount: Int?,
) {
    TextOnly(text = "Text only", icon = null, badgeCount = null),
    TextWithIcon(text = "Text with Icon", icon = Icons.Filled.Star, badgeCount = null),
    TextWithIconAndBadge(text = "Text with Icon and Badge", icon = Icons.Outlined.AllInbox, badgeCount = 10),
}
