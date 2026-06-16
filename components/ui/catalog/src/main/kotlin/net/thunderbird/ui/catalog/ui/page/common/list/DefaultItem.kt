package net.thunderbird.ui.catalog.ui.page.common.list

import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import java.util.UUID

fun LazyGridScope.defaultItem(content: @Composable LazyGridItemScope.() -> Unit) {
    item(
        key = UUID.randomUUID().toString(),
    ) {
        content()
    }
}
