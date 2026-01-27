package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo

@Composable
internal fun SettingList(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val windowSizeInfo = getWindowSizeInfo()
    val isLandscape = windowSizeInfo.screenWidth > windowSizeInfo.screenHeight
    val isCompactHeight = windowSizeInfo.screenHeightSizeClass == WindowSizeClass.Compact
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
