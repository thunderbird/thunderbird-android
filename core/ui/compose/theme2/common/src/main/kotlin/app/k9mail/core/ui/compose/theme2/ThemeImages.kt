package app.k9mail.core.ui.compose.theme2

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class ThemeImages(
    @DrawableRes val logo: Int,
)

internal val LocalThemeImages = staticCompositionLocalOf<ThemeImages> {
    error("No ThemeImages provided")
}
