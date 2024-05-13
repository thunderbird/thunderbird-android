package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonElevated
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilled
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonFilledTonal
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonIcon
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.ui.catalog.ui.common.list.defaultItemPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

@Suppress("LongMethod")
@OptIn(ExperimentalLayoutApi::class)
fun LazyGridScope.buttonItems() {
    sectionHeaderItem(text = "Button - Filled")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonFilled(text = "Enabled", onClick = { })
            ButtonFilled(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Button - Filled Tonal")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonFilledTonal(text = "Enabled", onClick = { })
            ButtonFilledTonal(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Button - Elevated")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonElevated(text = "Enabled", onClick = { })
            ButtonElevated(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Button - Outlined")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonOutlined(text = "Enabled", onClick = { })
            ButtonOutlined(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Button - Text")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonText(text = "Enabled", onClick = { })
            ButtonText(text = "Colored", onClick = { }, color = Color.Magenta)
            ButtonText(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionHeaderItem(text = "Button - Icon")
    item {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.padding(defaultItemPadding()),
        ) {
            ButtonIcon(onClick = { }, imageVector = Icons.Outlined.Info)
            ButtonIcon(onClick = { }, imageVector = Icons.Outlined.Info, enabled = false)
        }
    }
}
