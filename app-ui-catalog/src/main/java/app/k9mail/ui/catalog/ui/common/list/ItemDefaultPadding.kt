package app.k9mail.ui.catalog.ui.common.list

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import app.k9mail.core.ui.compose.theme2.MainTheme

fun Modifier.itemDefaultPadding(): Modifier = composed {
    padding(
        horizontal = MainTheme.spacings.double,
    )
}
