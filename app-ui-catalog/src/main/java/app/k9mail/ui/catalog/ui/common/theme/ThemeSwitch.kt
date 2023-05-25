package app.k9mail.ui.catalog.ui.common.theme

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant

@Composable
fun ThemeSwitch(
    theme: Theme,
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (theme) {
        Theme.K9 -> K9ThemeSwitch(
            themeVariant = themeVariant,
            content = content,
        )
        Theme.THUNDERBIRD -> ThunderbirdThemeSwitch(
            themeVariant = themeVariant,
            content = content,
        )
    }
}

@Composable
private fun K9ThemeSwitch(
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        ThemeVariant.LIGHT -> K9Theme(
            darkTheme = false,
            content = content,
        )
        ThemeVariant.DARK -> K9Theme(
            darkTheme = true,
            content = content,
        )
    }
}

@Composable
private fun ThunderbirdThemeSwitch(
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        ThemeVariant.LIGHT -> ThunderbirdTheme(
            darkTheme = false,
            content = content,
        )
        ThemeVariant.DARK -> ThunderbirdTheme(
            darkTheme = true,
            content = content,
        )
    }
}
