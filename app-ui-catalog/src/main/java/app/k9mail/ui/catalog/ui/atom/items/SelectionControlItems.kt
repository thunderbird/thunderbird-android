package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

fun LazyGridScope.selectionControlItems() {
    sectionHeaderItem(text = "Checkbox")
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
    sectionHeaderItem(text = "Switch")
    captionItem(caption = "Checked") {
        Switch(checked = true, onCheckedChange = {})
    }
    captionItem(caption = "Unchecked") {
        Switch(checked = false, onCheckedChange = {})
    }
    captionItem(caption = "Disabled Checked") {
        Switch(checked = true, onCheckedChange = {}, enabled = false)
    }
    captionItem(caption = "Disabled") {
        Switch(checked = false, onCheckedChange = {}, enabled = false)
    }
}

private fun LazyGridScope.captionItem(
    caption: String,
    content: @Composable () -> Unit,
) {
    item {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.itemDefaultPadding(),
        ) {
            content()
            TextBodySmall(text = caption)
        }
    }
}
