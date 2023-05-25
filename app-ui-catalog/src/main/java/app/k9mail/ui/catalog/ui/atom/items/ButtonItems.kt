package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

fun LazyGridScope.buttonItems() {
    sectionHeaderItem(text = "Buttons - Filled")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            Button(text = "Enabled", onClick = { })
            Button(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Buttons - Outlined")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            ButtonOutlined(text = "Enabled", onClick = { })
            ButtonOutlined(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Buttons - Text only")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            ButtonText(text = "Enabled", onClick = { })
            ButtonText(text = "Disabled", onClick = { }, enabled = false)
        }
    }
}
