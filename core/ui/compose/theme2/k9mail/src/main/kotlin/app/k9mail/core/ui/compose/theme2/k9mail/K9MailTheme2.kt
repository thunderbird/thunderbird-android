package app.k9mail.core.ui.compose.theme2.k9mail

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

@Composable
fun K9MailTheme2(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val images = ThemeImages(
        logo = R.drawable.core_ui_theme2_k9mail_logo,
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
        dynamicColor = dynamicColor,
        content = content,
    )
}
