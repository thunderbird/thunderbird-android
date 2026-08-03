package net.thunderbird.components.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import net.thunderbird.components.ui.catalog.ui.page.organism.items.banners.bannerGlobal
import net.thunderbird.components.ui.catalog.ui.page.organism.items.banners.bannerInline

fun LazyGridScope.bannerItems() {
    bannerGlobal()
    bannerInline()
}
