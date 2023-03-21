package app.k9mail.core.ui.compose.common.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns the current window size info based on current Configuration.
 */
@Composable
fun getWindowSizeInfo(): WindowSizeInfo {
    val configuration = LocalConfiguration.current

    return WindowSizeInfo(
        screenWidthSizeClass = WindowSizeClass.fromWidth(configuration.screenWidthDp),
        screenHeightSizeClass = WindowSizeClass.fromHeight(configuration.screenHeightDp),
        screenWidth = configuration.screenWidthDp.dp,
        screenHeight = configuration.screenHeightDp.dp,
    )
}

@Immutable
data class WindowSizeInfo(
    val screenWidthSizeClass: WindowSizeClass,
    val screenHeightSizeClass: WindowSizeClass,
    val screenWidth: Dp,
    val screenHeight: Dp,
)
