package app.k9mail.core.ui.compose.theme2

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

fun Color.toHarmonizedColor(target: Color) = Color(MaterialColors.harmonize(toArgb(), target.toArgb()))
