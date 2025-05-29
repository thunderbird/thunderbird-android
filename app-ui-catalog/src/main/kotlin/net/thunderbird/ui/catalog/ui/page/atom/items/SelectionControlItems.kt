package net.thunderbird.ui.catalog.ui.page.atom.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.Checkbox
import app.k9mail.core.ui.compose.designsystem.atom.RadioGroup
import app.k9mail.core.ui.compose.designsystem.atom.Switch
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem

@Suppress("LongMethod")
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

    sectionHeaderItem(text = "Radio Group")
    defaultItem {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(defaultItemPadding()),
        ) {
            TextTitleMedium(text = "Selected")
            RadioGroup(
                onClick = {},
                options = radioGroupChoice,
                optionTitle = { it.second },
                selectedOption = radioGroupChoice[0],
                modifier = Modifier
                    .padding(MainTheme.spacings.default)
                    .fillMaxWidth(),
            )
        }
    }

    defaultItem {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(defaultItemPadding()),
        ) {
            TextTitleMedium(text = "Unselected")
            RadioGroup(
                onClick = {},
                options = radioGroupChoice,
                optionTitle = { it.second },
                modifier = Modifier
                    .padding(MainTheme.spacings.default)
                    .fillMaxWidth(),
            )
        }
    }
}

private fun LazyGridScope.captionItem(
    caption: String,
    content: @Composable () -> Unit,
) {
    defaultItem {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            content()
            TextBodySmall(text = caption)
        }
    }
}

val radioGroupChoice = persistentListOf(
    Pair("1", "Alpha"),
    Pair("2", "Beta"),
    Pair("3", "Gamma"),
    Pair("4", "Delta"),
)
