package net.thunderbird.ui.catalog.ui.page.organism.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import net.thunderbird.ui.catalog.ui.page.organism.items.dialogs.alertDialogs
import net.thunderbird.ui.catalog.ui.page.organism.items.dialogs.basicDialogs

fun LazyGridScope.dialogItems() {
    basicDialogs()
    alertDialogs()
}
