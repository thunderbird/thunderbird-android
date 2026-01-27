package app.k9mail.core.ui.compose.theme2

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun MainTheme(
    themeConfig: ThemeConfig,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val themeColorScheme = selectThemeColorScheme(
        themeConfig = themeConfig,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
    )
    val themeImages = selectThemeImages(
        themeConfig = themeConfig,
        darkTheme = darkTheme,
    )

    CompositionLocalProvider(
        LocalThemeColorScheme provides themeColorScheme,
        LocalThemeElevations provides themeConfig.elevations,
        LocalThemeImages provides themeImages,
        LocalThemeShapes provides themeConfig.shapes,
        LocalThemeSizes provides themeConfig.sizes,
        LocalThemeSpacings provides themeConfig.spacings,
        LocalThemeTypography provides themeConfig.typography,
    ) {
        MaterialTheme(
            colorScheme = themeColorScheme.toMaterial3ColorScheme(),
            shapes = themeConfig.shapes.toMaterial3Shapes(),
            typography = themeConfig.typography.toMaterial3Typography(),
            content = content,
        )
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in
 * the hierarchy.
 */
object MainTheme {

    /**
     * Retrieves the current [ColorScheme] at the call site's position in the hierarchy.
     */
    val colors: ThemeColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeColorScheme.current

    /**
     * Retrieves the current [ThemeElevations] at the call site's position in the hierarchy.
     */
    val elevations: ThemeElevations
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeElevations.current

    /**
     * Retrieves the current [ThemeImages] at the call site's position in the hierarchy.
     */
    val images: ThemeImages
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeImages.current

    /**
     * Retrieves the current [ThemeShapes] at the call site's position in the hierarchy.
     */
    val shapes: ThemeShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeShapes.current

    /**
     * Retrieves the current [ThemeSizes] at the call site's position in the hierarchy.
     */
    val sizes: ThemeSizes
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeSizes.current

    /**
     * Retrieves the current [ThemeSpacings] at the call site's position in the hierarchy.
     */
    val spacings: ThemeSpacings
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeSpacings.current

    /**
     * Retrieves the current [ThemeTypography] at the call site's position in the hierarchy.
     */
    val typography: ThemeTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeTypography.current
}
