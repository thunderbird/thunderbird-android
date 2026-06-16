package net.thunderbird.components.ui.bolt.molecule.input

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import net.thunderbird.components.ui.bolt.theme.BoltTheme

@Composable
fun inputContentPadding(
    start: Dp = BoltTheme.spacings.double,
    top: Dp = BoltTheme.spacings.default,
    end: Dp = BoltTheme.spacings.double,
    bottom: Dp = BoltTheme.spacings.default,
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
)

@Composable
fun inputContentPadding(
    horizontal: Dp = BoltTheme.spacings.double,
    vertical: Dp = BoltTheme.spacings.default,
): PaddingValues = PaddingValues(
    horizontal = horizontal,
    vertical = vertical,
)
