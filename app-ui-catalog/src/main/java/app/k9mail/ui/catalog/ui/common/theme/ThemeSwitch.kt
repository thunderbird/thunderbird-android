package app.k9mail.ui.catalog.ui.common.theme

import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme.K9Theme
import app.k9mail.core.ui.compose.theme.ThunderbirdTheme
import app.k9mail.core.ui.compose.theme2.k9mail.K9MailTheme2
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

        Theme.THEME_2_K9 -> K9Theme2Switch(
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
        ) {
            K9MailTheme2(
                darkTheme = false,
                content = content,
            )
        }

        ThemeVariant.DARK -> K9Theme(
            darkTheme = true,
        ) {
            K9MailTheme2(
                darkTheme = true,
                content = content,
            )
        }
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
        ) {
            K9MailTheme2(
                darkTheme = true,
                content = content,
            )
        }

        ThemeVariant.DARK -> ThunderbirdTheme(
            darkTheme = true,
        ) {
            K9MailTheme2(
                darkTheme = true,
                content = content,
            )
        }
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
        ) {
            K9Theme(
                darkTheme = false,
                content = content,
            )
        }

        ThemeVariant.DARK -> K9MailTheme2(
            darkTheme = true,
        ) {
            K9Theme(
                darkTheme = false,
                content = content,
            )
        }
    }
}
