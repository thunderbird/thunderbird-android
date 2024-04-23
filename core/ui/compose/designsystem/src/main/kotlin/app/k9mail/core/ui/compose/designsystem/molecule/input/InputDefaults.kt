package app.k9mail.core.ui.compose.designsystem.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme2.MainTheme

@Composable
fun inputContentPadding(
    start: Dp = MainTheme.spacings.double,
    top: Dp = MainTheme.spacings.default,
    end: Dp = MainTheme.spacings.double,
    bottom: Dp = MainTheme.spacings.default,
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
)

@Composable
fun inputContentPadding(
    horizontal: Dp = MainTheme.spacings.double,
    vertical: Dp = MainTheme.spacings.default,
): PaddingValues = PaddingValues(
    horizontal = horizontal,
    vertical = vertical,
)
