package net.thunderbird.core.ui.compose.theme2.thunderbird

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.compose.theme2.ThemeColorSchemeVariants
import net.thunderbird.core.ui.compose.theme2.ThemeConfig
import net.thunderbird.core.ui.compose.theme2.ThemeImageVariants
import net.thunderbird.core.ui.compose.theme2.ThemeImages
import net.thunderbird.core.ui.compose.theme2.default.defaultThemeElevations
import net.thunderbird.core.ui.compose.theme2.default.defaultThemeShapes
import net.thunderbird.core.ui.compose.theme2.default.defaultThemeSizes
import net.thunderbird.core.ui.compose.theme2.default.defaultThemeSpacings
import net.thunderbird.core.ui.compose.theme2.default.defaultTypography
import net.thunderbird.core.ui.compose.theme2.resources.Res
import net.thunderbird.core.ui.compose.theme2.resources.core_ui_theme2_thunderbird_logo

@Composable
fun ThunderbirdTheme2(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val images = ThemeImages(
        logo = Res.drawable.core_ui_theme2_thunderbird_logo,
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
