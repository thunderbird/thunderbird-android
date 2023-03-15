package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption

fun LazyGridScope.selectionControlItems() {
    sectionHeaderItem(text = "Selection Controls")
    sectionSubtitleItem(text = "Checkbox")
    captionItem(caption = "Checked") {
        Checkbox(checked = true, onCheckedChange = {})
    }
    captionItem(caption = "Unchecked") {
        Checkbox(checked = false, onCheckedChange = {})
    }
    captionItem(caption = "Disabled Checked") {
        Checkbox(checked = true, onCheckedChange = {}, enabled = false)
    }
    captionItem(caption = "Disabled") {
        Checkbox(checked = false, onCheckedChange = {}, enabled = false)
    }
}

private fun LazyGridScope.captionItem(
    caption: String,
    content: @Composable () -> Unit,
) {
    item {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
            TextCaption(text = caption)
        }
    }
}
