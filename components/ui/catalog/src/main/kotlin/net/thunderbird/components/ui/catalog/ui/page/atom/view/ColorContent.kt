package net.thunderbird.components.ui.catalog.ui.page.atom.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextBodySmall
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.components.ui.catalog.ui.page.common.list.defaultItemPadding

@Composable
internal fun ColorContent(
    text: String,
    color: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color,
        modifier = Modifier
            .padding(defaultItemPadding())
            .then(modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(MainTheme.spacings.double),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            ) {
                TextBodyLarge(
                    text = text,
                    color = textColor,
                )
                TextBodySmall(
                    text = color.toHex(),
                    color = textColor,
                )
            }
        }
    }
}

private fun Color.toHex(): String {
    return "#${Integer.toHexString(toArgb()).uppercase()}"
}
