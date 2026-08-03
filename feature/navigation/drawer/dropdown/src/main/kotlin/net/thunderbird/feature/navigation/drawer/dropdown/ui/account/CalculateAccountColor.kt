package net.thunderbird.feature.navigation.drawer.dropdown.ui.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import net.thunderbird.components.ui.bolt.theme.BoltTheme
import net.thunderbird.components.ui.bolt.theme.toHarmonizedColor

/**
 * Calculates the account color based on the provided account color and surface color.
 *
 * If the account color is unspecified, it returns the fallback color.
 * Otherwise, it harmonizes the account color with the surface color.
 *
 * @param accountColor The color of the account.
 * @param fallbackColor The fallback color to use if the account color is unspecified.
 */
@Composable
internal fun rememberCalculatedAccountColor(
    accountColor: Color,
    fallbackColor: Color = BoltTheme.colors.primary,
): Color {
    val surfaceColor = BoltTheme.colors.surface
    return remember(accountColor, surfaceColor, fallbackColor) {
        if (accountColor == Color.Unspecified) {
            fallbackColor
        } else {
            accountColor.toHarmonizedColor(surfaceColor)
        }
    }
}
