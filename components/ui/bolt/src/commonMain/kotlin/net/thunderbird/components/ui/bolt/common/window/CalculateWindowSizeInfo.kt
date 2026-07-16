package net.thunderbird.components.ui.bolt.common.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize

@Composable
public fun calculateWindowSizeInfo(): WindowSizeInfo {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val size = with(density) { windowInfo.containerSize.toSize().toDpSize() }
    val windowSizeClass = WindowSizeClass.calculateFromSize(size)

    return remember(windowInfo) {
        WindowSizeInfo(
            sizeClass = windowSizeClass,
            size = size,
        )
    }
}
