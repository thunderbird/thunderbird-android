package app.k9mail.ui.catalog.ui.common.theme

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
import app.k9mail.core.ui.compose.theme2.thunderbird.ThunderbirdTheme2
import app.k9mail.ui.catalog.ui.CatalogContract.Theme
import app.k9mail.ui.catalog.ui.CatalogContract.ThemeVariant

@Composable
fun ThemeSwitch(
    theme: Theme,
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (theme) {
        Theme.THEME_2_K9 -> K9Theme2Switch(
            themeVariant = themeVariant,
            content = content,
        )

        Theme.THEME_2_THUNDERBIRD -> ThunderbirdTheme2Switch(
            themeVariant = themeVariant,
            content = content,
        )
    }
}

@Composable
private fun K9Theme2Switch(
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        ThemeVariant.LIGHT -> K9MailTheme2(
            darkTheme = false,
            content = content,
        )

        ThemeVariant.DARK -> K9MailTheme2(
            darkTheme = true,
            content = content,
        )
    }
}

@Composable
private fun ThunderbirdTheme2Switch(
    themeVariant: ThemeVariant,
    content: @Composable () -> Unit,
) {
    when (themeVariant) {
        ThemeVariant.LIGHT -> ThunderbirdTheme2(
            darkTheme = false,
            content = content,
        )

        ThemeVariant.DARK -> ThunderbirdTheme2(
            darkTheme = true,
            content = content,
        )
    }
}
