package net.thunderbird.components.ui.catalog.ui.common.theme

import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.k9mail.K9MailBoltTheme
import net.thunderbird.components.ui.bolt.theme.thunderbird.ThunderbirdBoltTheme
import net.thunderbird.components.ui.catalog.ui.CatalogContract.Theme
import net.thunderbird.components.ui.catalog.ui.CatalogContract.ThemeVariant

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
        ThemeVariant.LIGHT -> K9MailBoltTheme(
            darkTheme = false,
            content = content,
        )

        ThemeVariant.DARK -> K9MailBoltTheme(
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
        ThemeVariant.LIGHT -> ThunderbirdBoltTheme(
            darkTheme = false,
            content = content,
        )

        ThemeVariant.DARK -> ThunderbirdBoltTheme(
            darkTheme = true,
            content = content,
        )
    }
}
