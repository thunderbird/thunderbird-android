package net.thunderbird.components.ui.bolt.theme.k9mail

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.thunderbird.components.ui.bolt.theme.MainTheme
import net.thunderbird.components.ui.bolt.theme.ThemeColorSchemeVariants
import net.thunderbird.components.ui.bolt.theme.ThemeConfig
import net.thunderbird.components.ui.bolt.theme.ThemeImageVariants
import net.thunderbird.components.ui.bolt.theme.ThemeImages
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeElevations
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeShapes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSizes
import net.thunderbird.components.ui.bolt.theme.default.defaultThemeSpacings
import net.thunderbird.components.ui.bolt.theme.default.defaultTypography
import net.thunderbird.components.ui.bolt.resources.Res
import net.thunderbird.components.ui.bolt.resources.bolt_k9mail_logo

@Composable
fun K9MailTheme2(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = ThemeImages(
        logo = Res.drawable.bolt_k9mail_logo,
    )

    val themeConfig = ThemeConfig(
        colors = ThemeColorSchemeVariants(
            dark = darkThemeColorScheme,
            light = lightThemeColorScheme,
        ),
        elevations = defaultThemeElevations,
        images = ThemeImageVariants(
            light = images,
            dark = images,
        ),
        sizes = defaultThemeSizes,
        spacings = defaultThemeSpacings,
        shapes = defaultThemeShapes,
        typography = defaultTypography,
    )

    MainTheme(
        themeConfig = themeConfig,
        darkTheme = darkTheme,
        content = content,
    )
}
