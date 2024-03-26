package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

@OptIn(ExperimentalLayoutApi::class)
fun LazyGridScope.buttonItems() {
    sectionHeaderItem(text = "Buttons - Filled")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            ButtonFilled(text = "Enabled", onClick = { })
            ButtonFilled(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Buttons - Outlined")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            ButtonOutlined(text = "Enabled", onClick = { })
            ButtonOutlined(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Buttons - Text only")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.itemDefaultPadding(),
        ) {
            ButtonText(text = "Enabled", onClick = { })
            ButtonText(text = "Colored", onClick = { }, color = Color.Magenta)
            ButtonText(text = "Disabled", onClick = { }, enabled = false)
        }
    }
}
