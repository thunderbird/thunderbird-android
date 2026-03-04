package net.thunderbird.core.ui.common.window

import androidx.compose.runtime.Immutable

@Immutable
public data class WindowSizeInfo(
    val screenWidthSizeClass: WindowSizeClass,
    val screenHeightSizeClass: WindowSizeClass,
    val screenWidth: Int,
    val screenHeight: Int,
)
