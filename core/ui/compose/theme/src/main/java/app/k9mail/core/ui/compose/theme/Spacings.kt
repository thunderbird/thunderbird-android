package app.k9mail.core.ui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacings(
    val quarter: Dp = 2.dp,
    val half: Dp = 4.dp,
    val default: Dp = 8.dp,
    val oneHalf: Dp = 12.dp,
    val double: Dp = 16.dp,
    val triple: Dp = 24.dp,
    val quadruple: Dp = 32.dp,
)

internal val LocalSpacings = staticCompositionLocalOf { Spacings() }
