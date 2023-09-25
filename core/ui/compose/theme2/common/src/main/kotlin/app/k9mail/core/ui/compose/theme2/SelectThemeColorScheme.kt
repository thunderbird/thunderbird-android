package app.k9mail.core.ui.compose.theme2

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun selectThemeColorScheme(
    themeConfig: ThemeConfig,
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ThemeColorScheme {
    return when {
        dynamicColor && supportsDynamicColor() -> {
            val context = LocalContext.current
            val colorScheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            colorScheme.toThemeColorScheme()
        }

        darkTheme -> themeConfig.colors.dark
        else -> themeConfig.colors.light
    }
}

// Supported from Android 12+
private fun supportsDynamicColor(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
}

private fun ColorScheme.toThemeColorScheme() = ThemeColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,

    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,

    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,

    error = error,
    onError = onError,
    errorContainer = errorContainer,
    onErrorContainer = onErrorContainer,

    surface = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLowest = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    surfaceContainerHigh = surfaceContainerHigh,
    surfaceContainerHighest = surfaceContainerHighest,

    inverseSurface = inverseSurface,
    inverseOnSurface = inverseOnSurface,
    inversePrimary = inversePrimary,

    outline = outline,
    outlineVariant = outlineVariant,

    surfaceBright = surfaceBright,
    surfaceDim = surfaceDim,

    scrim = scrim,
)
