package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.common.window.WindowHeightSizeClass
import net.thunderbird.components.ui.bolt.common.window.calculateWindowSizeInfo

@Composable
internal fun SettingList(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val windowSizeInfo = calculateWindowSizeInfo()
    val isLandscape = windowSizeInfo.size.width > windowSizeInfo.size.height
    val isCompactHeight = windowSizeInfo.sizeClass.heightSizeClass == WindowHeightSizeClass.Compact
    val phoneLandscape = isLandscape && isCompactHeight

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
