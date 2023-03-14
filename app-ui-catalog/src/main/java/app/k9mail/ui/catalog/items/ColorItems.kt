package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBody1
import app.k9mail.core.ui.compose.theme.MainTheme

fun LazyGridScope.colorItems() {
    sectionHeaderItem(text = "Colors")
    item {
        ColorContent(
            name = "Primary",
            color = MainTheme.colors.primary,
        )
    }
    item {
        ColorContent(
            name = "Primary Variant",
            color = MainTheme.colors.primaryVariant,
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
            name = "Secondary Variant",
            color = MainTheme.colors.secondaryVariant,
        )
    }
    item {
        ColorContent(
            name = "Background",
            color = MainTheme.colors.background,
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
            name = "Error",
            color = MainTheme.colors.error,
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
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = modifier.padding(MainTheme.spacings.double),
        ) {
            TextBody1(text = name)
        }
    }
}
