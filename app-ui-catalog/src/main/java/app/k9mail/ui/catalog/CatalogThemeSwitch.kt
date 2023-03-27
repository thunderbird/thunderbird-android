package app.k9mail.ui.catalog

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme

@Composable
fun CatalogThemeSwitch(
    theme: CatalogTheme,
    themeVariant: CatalogThemeVariant,
    content: @Composable () -> Unit,
) {
    when (theme) {
        CatalogTheme.K9 -> K9ThemeSwitch(
            themeVariant = themeVariant,
            content = content,
        )
        CatalogTheme.THUNDERBIRD -> ThunderbirdThemeSwitch(
            themeVariant = themeVariant,
            content = content,
        )
    }
}

@Composable
private fun K9ThemeSwitch(
    themeVariant: CatalogThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        CatalogThemeVariant.LIGHT -> K9Theme(
            darkTheme = false,
            content = content,
        )
        CatalogThemeVariant.DARK -> K9Theme(
            darkTheme = true,
            content = content,
        )
    }
}

@Composable
private fun ThunderbirdThemeSwitch(
    themeVariant: CatalogThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        CatalogThemeVariant.LIGHT -> ThunderbirdTheme(
            darkTheme = false,
            content = content,
        )
        CatalogThemeVariant.DARK -> ThunderbirdTheme(
            darkTheme = true,
            content = content,
        )
    }
}
