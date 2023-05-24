package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant
import app.k9mail.ui.catalog.ui.common.theme.ThemeSelector
import app.k9mail.ui.catalog.ui.common.theme.ThemeVariantSelector

fun LazyGridScope.themeSelectorItems(
    theme: Theme,
    themeVariant: ThemeVariant,
    onThemeChange: () -> Unit,
    onThemeVariantChange: () -> Unit,
) {
    item {
        ThemeSelector(
            theme = theme,
            modifier = Modifier.fillMaxWidth(),
            onThemeChangeClick = onThemeChange,
        )
    }
    item {
        ThemeVariantSelector(
            themeVariant = themeVariant,
            modifier = Modifier.fillMaxWidth(),
            onThemeVariantChange = onThemeVariantChange,
        )
    }
}
