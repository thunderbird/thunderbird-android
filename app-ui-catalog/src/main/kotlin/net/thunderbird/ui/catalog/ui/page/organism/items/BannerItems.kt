package net.thunderbird.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import net.thunderbird.ui.catalog.ui.page.organism.items.banners.bannerGlobal
import net.thunderbird.ui.catalog.ui.page.organism.items.banners.bannerInline

fun LazyGridScope.bannerItems() {
    bannerGlobal()
    bannerInline()
}
