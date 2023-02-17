package app.k9mail.core.ui.compose.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class Images(
    @DrawableRes val logo: Int,
)

internal val LocalImages = staticCompositionLocalOf<Images> {
    error("No LocalImages defined")
}
