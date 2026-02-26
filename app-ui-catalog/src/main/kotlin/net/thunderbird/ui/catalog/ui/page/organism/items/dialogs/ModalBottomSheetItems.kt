package net.thunderbird.ui.catalog.ui.page.organism.items.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyMedium
import app.k9mail.core.ui.compose.designsystem.atom.text.TextTitleMedium
import app.k9mail.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.designsystem.organism.ModalBottomSheet
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItem
import net.thunderbird.ui.catalog.ui.page.common.list.defaultItemPadding
import net.thunderbird.ui.catalog.ui.page.common.list.sectionHeaderItem

internal fun LazyGridScope.modalBottomSheetItems() {
    sectionHeaderItem("Bottom sheets")
    modalBottomSheetSimpleItem()
}

private fun LazyGridScope.modalBottomSheetSimpleItem() {
    defaultItem {
        var showSheet by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            ButtonFilled(
                text = "Show ModalBottomSheet",
                onClick = { showSheet = true },
                modifier = Modifier.padding(defaultItemPadding()),
            )
        }

        if (showSheet) {
            ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = MainTheme.spacings.double),
                ) {
                    TextTitleMedium(text = "Modal bottom sheet")
                    TextBodyMedium(
                        text = "This is a simple preview of the design-system ModalBottomSheet.",
                        modifier = Modifier.padding(top = MainTheme.spacings.default),
                    )
                    ButtonFilled(
                        text = "Dismiss",
                        onClick = { showSheet = false },
                        modifier = Modifier.padding(top = MainTheme.spacings.double),
                    )
                }
            }
        }
    }
}
