package app.k9mail.core.ui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material.Colors as MaterialColors

@Immutable
data class Colors(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onError: Color,
    val isLight: Boolean,
)

internal fun lightColors(
    primary: Color = MaterialColor.deep_purple_600,
    primaryVariant: Color = MaterialColor.deep_purple_900,
    secondary: Color = MaterialColor.cyan_600,
    secondaryVariant: Color = MaterialColor.cyan_800,
    background: Color = MaterialColor.gray_200,
    surface: Color = Color.White,
    error: Color = MaterialColor.red_600,
    onPrimary: Color = Color.White,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.Black,
    onSurface: Color = Color.Black,
    onError: Color = Color.White,
) = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
    isLight = true,
)

internal fun darkColors(
    primary: Color = MaterialColor.deep_purple_200,
    primaryVariant: Color = MaterialColor.deep_purple_50,
    secondary: Color = MaterialColor.cyan_300,
    secondaryVariant: Color = MaterialColor.cyan_100,
    background: Color = MaterialColor.gray_800,
    surface: Color = MaterialColor.gray_900,
    error: Color = MaterialColor.red_300,
    onPrimary: Color = Color.Black,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.White,
    onSurface: Color = Color.White,
    onError: Color = Color.Black,
) = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
    isLight = false,
)

internal fun Colors.toMaterialColors(): MaterialColors {
    return MaterialColors(
        primary = primary,
        primaryVariant = primaryVariant,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        background = background,
        surface = surface,
        error = error,
        onPrimary = onPrimary,
        onSecondary = onSecondary,
        onBackground = onBackground,
        onSurface = onSurface,
        onError = onError,
        isLight = isLight,
    )
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }
