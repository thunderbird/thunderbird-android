package net.thunderbird.core.ui.common.padding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.common.window.WindowWidthSizeClass
import net.thunderbird.core.ui.common.window.calculateWindowSizeInfo

@Composable
public fun calculateResponsiveWidthPadding(): PaddingValues {
    val windowSizeInfo = calculateWindowSizeInfo()
    val horizontalPadding = when (windowSizeInfo.sizeClass.widthSizeClass) {
        WindowWidthSizeClass.Small, WindowWidthSizeClass.Compact -> 0.dp
        WindowWidthSizeClass.Medium -> ((windowSizeInfo.size.width - WindowWidthSizeClass.BREAKPOINT_COMPACT) / 2)
        WindowWidthSizeClass.Expanded -> ((windowSizeInfo.size.width - WindowWidthSizeClass.BREAKPOINT_MEDIUM) / 2)
    }
    return PaddingValues(horizontal = horizontalPadding)
}
