package app.k9mail.core.ui.compose.designsystem.atom.button

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import app.k9mail.core.ui.compose.theme.MainTheme

@Composable
fun buttonContentPadding(
    start: Dp = MainTheme.spacings.quadruple,
    top: Dp = MainTheme.spacings.default,
    end: Dp = MainTheme.spacings.quadruple,
    bottom: Dp = MainTheme.spacings.default,
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
)
