package app.k9mail.core.ui.compose.theme2.thunderbird

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import app.k9mail.core.ui.compose.theme2.MainTheme
import app.k9mail.core.ui.compose.theme2.ThemeColorSchemeVariants
import app.k9mail.core.ui.compose.theme2.ThemeConfig
import app.k9mail.core.ui.compose.theme2.ThemeImageVariants
import app.k9mail.core.ui.compose.theme2.ThemeImages
import app.k9mail.core.ui.compose.theme2.default.defaultThemeElevations
import app.k9mail.core.ui.compose.theme2.default.defaultThemeShapes
import app.k9mail.core.ui.compose.theme2.default.defaultThemeSizes
import app.k9mail.core.ui.compose.theme2.default.defaultThemeSpacings
import app.k9mail.core.ui.compose.theme2.default.defaultTypography
import tfa.core.ui.compose.theme2.thunderbird.generated.resources.Res
import tfa.core.ui.compose.theme2.thunderbird.generated.resources.core_ui_theme2_thunderbird_logo

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
