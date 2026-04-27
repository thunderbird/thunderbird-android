package net.thunderbird.core.ui.common.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
public data class WindowSizeInfo(
    val screenWidthSizeClass: WindowSizeClass,
    val screenHeightSizeClass: WindowSizeClass,
    val screenWidth: Dp,
    val screenHeight: Dp,
)
