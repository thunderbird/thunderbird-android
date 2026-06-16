package net.thunderbird.components.ui.catalog.ui.page.common.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

fun LazyGridScope.sectionSubtitleItem(
    text: String,
) {
    fullSpanItem {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = BoltTheme.spacings.double,
                    top = BoltTheme.spacings.default,
                    end = BoltTheme.spacings.double,
                ),
        ) {
            TextTitleMedium(
                text = text,
            )
            DividerHorizontal()
        }
    }
}
