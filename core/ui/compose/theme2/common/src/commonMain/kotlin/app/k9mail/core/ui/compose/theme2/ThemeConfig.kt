package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Immutable

@Immutable
data class ThemeConfig(
    val colors: ThemeColorSchemeVariants,
    val elevations: ThemeElevations,
    val images: ThemeImageVariants,
    val shapes: ThemeShapes,
    val sizes: ThemeSizes,
    val spacings: ThemeSpacings,
    val typography: ThemeTypography,
)

@Immutable
data class ThemeColorSchemeVariants(
    val dark: ThemeColorScheme,
    val light: ThemeColorScheme,
)

@Immutable
data class ThemeImageVariants(
    val dark: ThemeImages,
    val light: ThemeImages,
)
