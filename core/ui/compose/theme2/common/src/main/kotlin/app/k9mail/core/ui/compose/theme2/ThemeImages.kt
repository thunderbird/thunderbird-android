package app.k9mail.core.ui.compose.theme2

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
@Immutable
data class ThemeImages(
    @param:DrawableRes val logo: Int,
)

internal val LocalThemeImages = staticCompositionLocalOf<ThemeImages> {
    error("No ThemeImages provided")
}
