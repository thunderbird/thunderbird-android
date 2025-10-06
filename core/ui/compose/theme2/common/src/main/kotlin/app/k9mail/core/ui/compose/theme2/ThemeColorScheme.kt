package app.k9mail.core.ui.compose.theme2

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor as Material3LocalContentColor

/**
 * Theme color scheme following Material 3 color roles.
 *
 * This supports tone-based Surfaces introduced for Material 3.
 *
 * @see: https://m3.material.io/styles/color/roles
 * @see: https://material.io/blog/tone-based-surface-color-m3
 */
@Immutable
data class ThemeColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,

    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,

    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,

    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,

    val surfaceDim: Color,
    val surface: Color,
    val surfaceBright: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,

    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,

    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,

    val outline: Color,
    val outlineVariant: Color,

    val scrim: Color,

    // extra colors
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,

    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,

    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
)

/**
 * Convert a [ThemeColorScheme] to a Material 3 [ColorScheme].
 *
 * Note: background, onBackground are deprecated and mapped to surface, onSurface.
 */
internal fun ThemeColorScheme.toMaterial3ColorScheme(): ColorScheme {
    return ColorScheme(
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

        surfaceDim = surfaceDim,
        surface = surface,
        surfaceBright = surfaceBright,
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

        scrim = scrim,

        // Remapping properties due to changes in Material 3 tone based surface colors
        // https://material.io/blog/tone-based-surface-color-m3
        background = surface,
        onBackground = onSurface,
        surfaceVariant = surfaceContainerHighest,

        surfaceTint = surfaceContainerHighest,
    )
}

internal val LocalThemeColorScheme = staticCompositionLocalOf<ThemeColorScheme> {
    error("No ThemeColorScheme provided")
}

/**
 * CompositionLocal used to specify the default color for text and icons.
 *
 * This uses the Material 3 [LocalContentColor] implementation.
 */
val LocalContentColor get() = Material3LocalContentColor
