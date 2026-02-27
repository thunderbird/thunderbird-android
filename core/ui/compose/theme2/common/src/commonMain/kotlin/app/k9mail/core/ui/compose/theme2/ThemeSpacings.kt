package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

@Immutable
data class ThemeSpacings(
    val zero: Dp,
    val quarter: Dp,
    val half: Dp,
    val default: Dp,
    val oneHalf: Dp,
    val double: Dp,
    val triple: Dp,
    val quadruple: Dp,
)

internal val LocalThemeSpacings = staticCompositionLocalOf<ThemeSpacings> {
    error("No ThemeSpacings provided")
}
