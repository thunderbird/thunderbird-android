package net.thunderbird.components.ui.bolt.common.window

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.DpSize

/**
 * Window size information.
 *
 * @param sizeClass the window size class
 * @param size the window size
 */
@Immutable
public data class WindowSizeInfo(
    val sizeClass: WindowSizeClass,
    val size: DpSize,
)
