package app.k9mail.core.ui.compose.theme2

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.google.android.material.color.MaterialColors

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
            colorScheme.toDynamicThemeColorScheme(darkTheme, themeConfig.colors)
        }

        darkTheme -> themeConfig.colors.dark
        else -> themeConfig.colors.light
    }
}

// Supported from Android 12+
private fun supportsDynamicColor(): Boolean {
    return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
}

@Suppress("LongMethod")
private fun ColorScheme.toDynamicThemeColorScheme(
    darkTheme: Boolean,
    colors: ThemeColorSchemeVariants,
): ThemeColorScheme {
    val colorScheme = if (darkTheme) colors.dark else colors.light

    val info = colorScheme.info.toHarmonizedColor(primary)
    val onInfo = colorScheme.onInfo.toHarmonizedColor(primary)
    val infoContainer = colorScheme.infoContainer.toHarmonizedColor(primary)
    val onInfoContainer = colorScheme.onInfoContainer.toHarmonizedColor(primary)

    val success = colorScheme.success.toHarmonizedColor(primary)
    val onSuccess = colorScheme.onSuccess.toHarmonizedColor(primary)
    val successContainer = colorScheme.successContainer.toHarmonizedColor(primary)
    val onSuccessContainer = colorScheme.onSuccessContainer.toHarmonizedColor(primary)

    val warning = colorScheme.warning.toHarmonizedColor(primary)
    val onWarning = colorScheme.onWarning.toHarmonizedColor(primary)
    val warningContainer = colorScheme.warningContainer.toHarmonizedColor(primary)
    val onWarningContainer = colorScheme.onWarningContainer.toHarmonizedColor(primary)

    return ThemeColorScheme(
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

        info = info,
        onInfo = onInfo,
        infoContainer = infoContainer,
        onInfoContainer = onInfoContainer,

        success = success,
        onSuccess = onSuccess,
        successContainer = successContainer,
        onSuccessContainer = onSuccessContainer,

        warning = warning,
        onWarning = onWarning,
        warningContainer = warningContainer,
        onWarningContainer = onWarningContainer,
    )
}

/**
 * The color roles of a theme accent color. They are used to define the main accent color and its complementary colors
 * in a Material Design theme.
 *
 * These roles are used to create a harmonious color scheme that works well together.
 *
 * The roles are:
 * - `accent`: The main accent color.
 * - `onAccent`: The color used for text and icons on top of the accent color.
 * - `accentContainer`: A container color that complements the accent color.
 * - `onAccentContainer`: The color used for text and icons on top of the accent container color.
 *
 * @param accent The main accent color.
 * @param onAccent The color used for text and icons on top of the accent color.
 * @param accentContainer A container color that complements the accent color.
 * @param onAccentContainer The color used for text and icons on top of the accent container color.
 */
data class ColorRoles(
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
)

/**
 * Returns a harmonized color that is derived from the given color and the target color.
 *
 * This function uses Material Colors to harmonize the two colors.
 *
 * @param target The target color to harmonize with.
 * @return A new color that is harmonized with the target color.
 */
fun Color.toHarmonizedColor(target: Color) = Color(MaterialColors.harmonize(toArgb(), target.toArgb()))

/**
 * Returns a [ColorRoles] object that contains the accent colors derived from the given color.
 *
 * This function uses Material Colors to retrieve the accent colors based on the provided color.
 *
 * @param context The context to use for retrieving the color roles.
 * @return A [ColorRoles] object containing the accent colors.
 */
fun Color.toColorRoles(context: Context): ColorRoles {
    val colorRoles = MaterialColors.getColorRoles(context, this.toArgb())
    return ColorRoles(
        accent = Color(colorRoles.accent),
        onAccent = Color(colorRoles.onAccent),
        accentContainer = Color(colorRoles.accentContainer),
        onAccentContainer = Color(colorRoles.onAccentContainer),
    )
}

/**
 * Returns a surface container color that is a composite of the given color and the theme surface container color.
 *
 * The alpha value is applied to the given color before compositing.
 *
 * @param alpha The alpha value to apply to the color.
 * @return A new color that is a composite of the given color and the theme surface container color.
 */
@Composable
fun Color.toSurfaceContainer(alpha: Float): Color {
    val color = copy(alpha = alpha)
    return color.compositeOver(MainTheme.colors.surfaceContainer)
}
