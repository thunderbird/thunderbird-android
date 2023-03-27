package app.k9mail.core.ui.compose.theme.color

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
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onMessage: Color,
    val toolbar: Color,
    val isLight: Boolean,
)

@Suppress("LongParameterList")
internal fun lightColors(
    primary: Color = MaterialColor.deep_purple_600,
    primaryVariant: Color = MaterialColor.deep_purple_900,
    secondary: Color = MaterialColor.cyan_600,
    secondaryVariant: Color = MaterialColor.cyan_800,
    background: Color = Color.White,
    surface: Color = Color.White,
    success: Color = MaterialColor.green_600,
    error: Color = MaterialColor.red_600,
    warning: Color = MaterialColor.orange_600,
    info: Color = MaterialColor.yellow_600,
    onPrimary: Color = Color.White,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.Black,
    onSurface: Color = Color.Black,
    onMessage: Color = Color.White,
    toolbar: Color = primary,
) = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    success = success,
    error = error,
    warning = warning,
    info = info,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onMessage = onMessage,
    toolbar = toolbar,
    isLight = true,
)

@Suppress("LongParameterList")
internal fun darkColors(
    primary: Color = MaterialColor.deep_purple_200,
    primaryVariant: Color = MaterialColor.deep_purple_50,
    secondary: Color = MaterialColor.cyan_300,
    secondaryVariant: Color = secondary,
    background: Color = MaterialColor.gray_950,
    surface: Color = MaterialColor.gray_950,
    success: Color = MaterialColor.green_300,
    error: Color = MaterialColor.red_300,
    warning: Color = MaterialColor.orange_300,
    info: Color = MaterialColor.yellow_300,
    onPrimary: Color = Color.Black,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.White,
    onSurface: Color = Color.White,
    onMessage: Color = Color.Black,
    toolbar: Color = surface,
) = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    success = success,
    error = error,
    warning = warning,
    info = info,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onMessage = onMessage,
    toolbar = toolbar,
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
        onError = onMessage,
        isLight = isLight,
    )
}

internal val LocalColors = staticCompositionLocalOf { lightColors() }
