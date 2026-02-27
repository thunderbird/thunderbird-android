package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun selectThemeColorScheme(
    themeConfig: ThemeConfig,
    darkTheme: Boolean,
): ThemeColorScheme {
    return when {
        darkTheme -> themeConfig.colors.dark
        else -> themeConfig.colors.light
    }
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
