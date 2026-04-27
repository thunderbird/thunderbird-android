package net.thunderbird.core.ui.compose.theme2

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.DrawableResource

@Immutable
data class ThemeImages(
    val logo: DrawableResource,
)

internal val LocalThemeImages = staticCompositionLocalOf<ThemeImages> {
    error("No ThemeImages provided")
}
