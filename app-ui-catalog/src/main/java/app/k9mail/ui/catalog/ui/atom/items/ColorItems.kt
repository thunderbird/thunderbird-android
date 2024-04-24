package app.k9mail.ui.catalog.ui.atom.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.ui.catalog.ui.common.list.itemDefaultPadding
import app.k9mail.ui.catalog.ui.common.list.sectionHeaderItem

@Suppress("LongMethod")
fun LazyGridScope.colorItems() {
    sectionHeaderItem(text = "Material theme colors")
    item {
        ColorContent(
            name = "Primary",
            color = MainTheme.colors.primary,
        )
    }
    item {
        ColorContent(
            name = "Secondary",
            color = MainTheme.colors.secondary,
        )
    }
    item {
        ColorContent(
            name = "Surface",
            color = MainTheme.colors.surface,
        )
    }
    item {
        ColorContent(
            name = "Success",
            color = MainTheme.colors.success,
        )
    }
    item {
        ColorContent(
            name = "Error",
            color = MainTheme.colors.error,
        )
    }
    item {
        ColorContent(
            name = "Warning",
            color = MainTheme.colors.warning,
        )
    }
    item {
        ColorContent(
            name = "Info",
            color = MainTheme.colors.info,
        )
    }
}

@Composable
private fun ColorContent(
    name: String,
    color: Color,
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
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(MainTheme.spacings.double),
        ) {
            TextBodyLarge(text = name)
        }
    }
}
