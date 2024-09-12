package app.k9mail.core.ui.compose.theme2

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp

@Immutable
data class ThemeSizes(
    val smaller: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val larger: Dp,
    val huge: Dp,
    val huger: Dp,

    val iconSmall: Dp,
    val icon: Dp,
    val iconLarge: Dp,

    val topBarHeight: Dp,
    val bottomBarHeight: Dp,
    val bottomBarHeightWithFab: Dp,
)

internal val LocalThemeSizes = staticCompositionLocalOf<ThemeSizes> {
    error("No ThemeSizes provided")
}
