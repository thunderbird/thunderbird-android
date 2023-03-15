package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.LazyGridScope
import app.k9mail.core.ui.compose.designsystem.atom.button.Button
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonOutlined
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.theme.MainTheme

fun LazyGridScope.buttonItems() {
    sectionHeaderItem(text = "Buttons")
    sectionSubtitleItem(text = "Contained")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            Button(text = "Enabled", onClick = { })
            Button(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionSubtitleItem(text = "Outlined")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            ButtonOutlined(text = "Enabled", onClick = { })
            ButtonOutlined(text = "Disabled", onClick = { }, enabled = false)
        }
    }
    sectionSubtitleItem(text = "Text")
    item {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
        ) {
            ButtonText(text = "Enabled", onClick = { })
            ButtonText(text = "Disabled", onClick = { }, enabled = false)
        }
    }
}
