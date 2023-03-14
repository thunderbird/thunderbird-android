package app.k9mail.ui.catalog

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun CatalogThemeSwitch(
    theme: CatalogTheme,
    themeVariation: CatalogThemeVariant,
    content: @Composable () -> Unit,
) {
    when (theme) {
        CatalogTheme.K9 -> K9Theme(
            darkTheme = isDarkVariation(themeVariation),
            content = content,
        )
        CatalogTheme.THUNDERBIRD -> ThunderbirdTheme(
            darkTheme = isDarkVariation(themeVariation),
            content = content,
        )
    }
}

private fun isDarkVariation(themeVariation: CatalogThemeVariant): Boolean =
    themeVariation == CatalogThemeVariant.DARK
