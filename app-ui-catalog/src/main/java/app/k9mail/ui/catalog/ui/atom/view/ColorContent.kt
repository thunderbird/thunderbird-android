package app.k9mail.ui.catalog.ui.atom.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodySmall
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding

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
            .itemDefaultPadding()
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
