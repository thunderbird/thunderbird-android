package net.thunderbird.components.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.card.CardElevated
import net.thunderbird.components.ui.bolt.atom.card.CardFilled
import net.thunderbird.components.ui.bolt.atom.card.CardOutlined
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.sectionHeaderItem
import net.thunderbird.components.ui.catalog.ui.page.common.list.wideItem

fun LazyGridScope.cardItems() {
    sectionCardElevated()
    sectionCardFilled()
    sectionCardOutlined()
}

fun LazyGridScope.sectionCardElevated() {
    sectionHeaderItem(text = "Card - Elevated")
    wideItem {
        CardElevated(
            modifier = Modifier.padding(horizontal = MainTheme.spacings.triple),
        ) {
            Box(
                modifier = Modifier.padding(MainTheme.spacings.triple),
            ) {
                TextBodyLarge(text = "Inside a CardElevated")
            }
        }
    }
}

fun LazyGridScope.sectionCardFilled() {
    sectionHeaderItem(text = "Card - Filled")
    wideItem {
        CardFilled(
            modifier = Modifier.padding(horizontal = MainTheme.spacings.triple),
        ) {
            Box(
                modifier = Modifier.padding(MainTheme.spacings.triple),
            ) {
                TextBodyLarge(text = "Inside a CardFilled")
            }
        }
    }
}

fun LazyGridScope.sectionCardOutlined() {
    sectionHeaderItem(text = "Card - Outlined")
    wideItem {
        CardOutlined(
            modifier = Modifier.padding(horizontal = MainTheme.spacings.triple),
        ) {
            Box(
                modifier = Modifier.padding(MainTheme.spacings.triple),
            ) {
                TextBodyLarge(text = "Inside a CardOutlined")
            }
        }
    }
}
