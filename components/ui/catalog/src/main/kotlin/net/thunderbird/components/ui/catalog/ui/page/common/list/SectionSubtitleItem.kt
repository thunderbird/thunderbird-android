package net.thunderbird.components.ui.catalog.ui.page.common.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import net.thunderbird.components.ui.bolt.atom.DividerHorizontal
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.MainTheme

fun LazyGridScope.sectionSubtitleItem(
    text: String,
) {
    fullSpanItem {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = MainTheme.spacings.double,
                    top = MainTheme.spacings.default,
                    end = MainTheme.spacings.double,
                ),
        ) {
            TextTitleMedium(
                text = text,
            )
            DividerHorizontal()
        }
    }
}
