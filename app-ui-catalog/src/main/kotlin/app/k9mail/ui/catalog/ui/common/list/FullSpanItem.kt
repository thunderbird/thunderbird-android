package app.k9mail.ui.catalog.ui.common.list

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import java.util.UUID

fun LazyGridScope.fullSpanItem(content: @Composable LazyGridItemScope.() -> Unit) {
    item(
        key = UUID.randomUUID().toString(),
        span = { GridItemSpan(maxLineSpan) },
    ) {
        content()
    }
}
