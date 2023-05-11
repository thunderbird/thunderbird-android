package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import app.k9mail.core.ui.compose.designsystem.atom.Icon
import app.k9mail.core.ui.compose.designsystem.atom.text.TextCaption
import app.k9mail.core.ui.compose.theme.Icons
import app.k9mail.core.ui.compose.theme.MainTheme

fun LazyGridScope.iconItems() {
    sectionHeaderItem(text = "Icons")
    item {
        IconItem(
            name = "Error",
            imageVector = Icons.error,
        )
    }
}

@Composable
private fun IconItem(
    name: String,
    imageVector: ImageVector,
) {
    Column(
        modifier = Modifier.padding(MainTheme.spacings.default),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
    ) {
        Row {
            Icon(
                imageVector = imageVector,
            )
            Icon(
                imageVector = imageVector,
                tint = Color.Magenta,
            )
        }
        TextCaption(text = name)
    }
}
