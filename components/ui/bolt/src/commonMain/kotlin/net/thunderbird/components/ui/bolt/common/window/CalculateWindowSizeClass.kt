package net.thunderbird.components.ui.bolt.common.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.toSize

/**
 * Calculates the current window size class based on the available screen size.
 *
 * This is taken from Compose's WindowSizeClass calculation logic, adapted for our use case.
 */
@Composable
public fun calculateWindowSizeClass(): WindowSizeClass {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val size = with(density) { windowInfo.containerSize.toSize().toDpSize() }
    return WindowSizeClass.calculateFromSize(size)
}
