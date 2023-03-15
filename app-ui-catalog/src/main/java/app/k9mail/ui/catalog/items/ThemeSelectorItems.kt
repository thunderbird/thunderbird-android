package app.k9mail.ui.catalog.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.Modifier
import app.k9mail.ui.catalog.CatalogTheme
import app.k9mail.ui.catalog.CatalogThemeSelector
import app.k9mail.ui.catalog.CatalogThemeVariant
import app.k9mail.ui.catalog.CatalogThemeVariantSelector

fun LazyGridScope.themeSelectorItems(
    catalogTheme: CatalogTheme,
    catalogThemeVariant: CatalogThemeVariant,
    onThemeChange: () -> Unit,
    onThemeVariantChange: () -> Unit,
) {
    item {
        CatalogThemeSelector(
            catalogTheme = catalogTheme,
            modifier = Modifier.fillMaxWidth(),
            onThemeChangeClick = onThemeChange,
        )
    }
    item {
        CatalogThemeVariantSelector(
            catalogThemeVariant = catalogThemeVariant,
            modifier = Modifier.fillMaxWidth(),
            onThemeVariantChange = onThemeVariantChange,
        )
    }
}
