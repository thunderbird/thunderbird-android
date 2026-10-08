package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.common.window.WindowHeightSizeClass
import net.thunderbird.components.ui.bolt.common.window.WindowWidthSizeClass
import net.thunderbird.components.ui.bolt.common.window.calculateWindowSizeInfo

@Composable
internal fun SettingList(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val windowSizeInfo = calculateWindowSizeInfo()
    val isLandscape = windowSizeInfo.size.width > windowSizeInfo.size.height
    val isCompactHeight = windowSizeInfo.sizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val isSmallDisplay = windowSizeInfo.sizeClass.widthSizeClass == WindowWidthSizeClass.Small ||
        windowSizeInfo.sizeClass.heightSizeClass == WindowHeightSizeClass.Small
    val phoneLandscape = isLandscape && isCompactHeight

    if (isSmallDisplay) {
        LazyHorizontalGrid(
            GridCells.Adaptive(minSize = 64.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            content()
        }
    } else {
        LazyVerticalGrid(
            columns = if (phoneLandscape) {
                GridCells.Adaptive(minSize = 64.dp)
            } else {
                GridCells.Adaptive(minSize = 200.dp)
            },
            modifier = modifier,
        ) {
            content()
        }
    }
}
