package app.k9mail.core.ui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Elevations(
    val card: Dp = 0.dp,
)

internal val LocalElevations = staticCompositionLocalOf { Elevations() }
