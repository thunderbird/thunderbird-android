package net.thunderbird.core.ui.common.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
public fun calculateWindowSizeInfo(): WindowSizeInfo {
    val windowInfo = LocalWindowInfo.current

    return remember(windowInfo) {
        WindowSizeInfo(
            screenWidthSizeClass = WindowSizeClass.fromWidth(windowInfo.containerSize.width),
            screenHeightSizeClass = WindowSizeClass.fromHeight(windowInfo.containerSize.height),
            screenWidth = windowInfo.containerDpSize.width,
            screenHeight = windowInfo.containerDpSize.height,
        )
    }
}
