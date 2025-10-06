package app.k9mail.core.ui.compose.common.padding

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo

@Composable
fun calculateResponsiveWidthPadding(): PaddingValues {
    val windowSizeInfo = getWindowSizeInfo()
    val horizontalPadding = when (windowSizeInfo.screenWidthSizeClass) {
        WindowSizeClass.Compact -> 0.dp

        WindowSizeClass.Medium -> (windowSizeInfo.screenWidth - WindowSizeClass.COMPACT_MAX_WIDTH.dp) / 2

        WindowSizeClass.Expanded -> (windowSizeInfo.screenWidth - WindowSizeClass.MEDIUM_MAX_WIDTH.dp) / 2
    }
    return PaddingValues(horizontal = horizontalPadding)
}
