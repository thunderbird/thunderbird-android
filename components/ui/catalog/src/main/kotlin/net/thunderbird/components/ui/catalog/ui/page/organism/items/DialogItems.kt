package net.thunderbird.components.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import net.thunderbird.components.ui.catalog.ui.page.organism.items.dialogs.alertDialogs
import net.thunderbird.components.ui.catalog.ui.page.organism.items.dialogs.basicDialogs
import net.thunderbird.components.ui.catalog.ui.page.organism.items.dialogs.modalBottomSheetItems

fun LazyGridScope.dialogItems() {
    basicDialogs()
    alertDialogs()
    modalBottomSheetItems()
}
