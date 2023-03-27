package app.k9mail.ui.catalog.items

import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconError
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconInfo
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconSuccess
import app.k9mail.core.ui.compose.designsystem.atom.icon.IconWarning

fun LazyGridScope.iconItems() {
    sectionHeaderItem(text = "Icons")
    item { IconSuccess() }
    item { IconError() }
    item { IconWarning() }
    item { IconInfo() }
}
