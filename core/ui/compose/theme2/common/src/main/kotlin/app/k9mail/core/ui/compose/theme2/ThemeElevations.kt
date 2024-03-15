package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation values used in the app.
 *
 * Material uses six levels of elevation, each with a corresponding dp value. These values are named for their
 * relative distance above the UI’s surface: 0, +1, +2, +3, +4, and +5. An element’s resting state can be on
 * levels 0 to +3, while levels +4 and +5 are reserved for user-interacted states such as hover and dragged.
 *
 * @see: https://m3.material.io/styles/elevation/tokens
 */
@Immutable
data class ThemeElevations(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

internal val LocalThemeElevations = staticCompositionLocalOf<ThemeElevations> {
    error("No ThemeElevations provided")
}
