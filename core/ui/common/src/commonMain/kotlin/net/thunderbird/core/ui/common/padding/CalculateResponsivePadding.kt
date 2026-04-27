package net.thunderbird.core.ui.common.padding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.common.window.WindowSizeClass
import net.thunderbird.core.ui.common.window.calculateWindowSizeInfo

@Composable
public fun calculateResponsiveWidthPadding(): PaddingValues {
    val windowSizeInfo = calculateWindowSizeInfo()
    val horizontalPadding = when (windowSizeInfo.screenWidthSizeClass) {
        WindowSizeClass.Small, WindowSizeClass.Compact -> 0.dp

        WindowSizeClass.Medium -> (windowSizeInfo.screenWidth - WindowSizeClass.COMPACT_MAX_WIDTH.dp) / 2

        WindowSizeClass.Expanded -> (windowSizeInfo.screenWidth - WindowSizeClass.MEDIUM_MAX_WIDTH.dp) / 2
    }
    return PaddingValues(horizontal = horizontalPadding)
}
