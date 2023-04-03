package app.k9mail.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.DevicePreviews
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.designsystem.template.ResponsiveContent
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.MainTheme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.ui.catalog.items.buttonItems
import app.k9mail.ui.catalog.items.colorItems
import app.k9mail.ui.catalog.items.imageItems
import app.k9mail.ui.catalog.items.selectionControlItems
import app.k9mail.ui.catalog.items.textFieldItems
import app.k9mail.ui.catalog.items.themeHeaderItem
import app.k9mail.ui.catalog.items.themeSelectorItems
import app.k9mail.ui.catalog.items.typographyItems

@Composable
fun CatalogContent(
    catalogTheme: CatalogTheme,
    catalogThemeVariant: CatalogThemeVariant,
    onThemeChange: () -> Unit,
    onThemeVariantChange: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Surface {
        ResponsiveContent {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                contentPadding = contentPadding,
                horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
                modifier = modifier.padding(MainTheme.spacings.double),
            ) {
                themeHeaderItem(text = "Thunderbird Catalog")
                themeSelectorItems(
                    catalogTheme = catalogTheme,
                    catalogThemeVariant = catalogThemeVariant,
                    onThemeChange = onThemeChange,
                    onThemeVariantChange = onThemeVariantChange,
                )

                typographyItems()
                colorItems()
                buttonItems()
                selectionControlItems()
                textFieldItems()
                imageItems()
            }
        }
    }
}

@DevicePreviews
@Composable
internal fun CatalogContentK9ThemePreview() {
    K9Theme {
        CatalogContent(
            catalogTheme = CatalogTheme.K9,
            catalogThemeVariant = CatalogThemeVariant.LIGHT,
            onThemeChange = {},
            onThemeVariantChange = {},
            contentPadding = PaddingValues(),
        )
    }
}

@DevicePreviews
@Composable
internal fun CatalogContentThunderbirdThemePreview() {
    ThunderbirdTheme {
        CatalogContent(
            catalogTheme = CatalogTheme.THUNDERBIRD,
            catalogThemeVariant = CatalogThemeVariant.LIGHT,
            onThemeChange = {},
            onThemeVariantChange = {},
            contentPadding = PaddingValues(),
        )
    }
}
