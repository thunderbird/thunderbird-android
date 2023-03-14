package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.text.TextHeadline6
import app.k9mail.core.ui.compose.theme.MainTheme

fun LazyGridScope.sectionHeaderItem(
    text: String,
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        TextHeadline6(text = text, modifier = Modifier.padding(top = MainTheme.spacings.default))
    }
}
