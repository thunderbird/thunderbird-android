package net.thunderbird.feature.navigation.drawer.dropdown.ui.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// As long as we use DrawerLayout, we don't have to worry about screens narrower than DRAWER_WIDTH. DrawerLayout will
// automatically limit the width of the content view so there's still room for a scrim with minimum tap width.
internal val DRAWER_WIDTH = 360.dp

@Composable
internal fun getAdditionalWidth(): Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    return if (isRtl) {
        WindowInsets.displayCutout.getRight(density = density, layoutDirection = layoutDirection)
    } else {
        WindowInsets.displayCutout.getLeft(density = density, layoutDirection = layoutDirection)
    }.pxToDp()
}

@Composable
private fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }
