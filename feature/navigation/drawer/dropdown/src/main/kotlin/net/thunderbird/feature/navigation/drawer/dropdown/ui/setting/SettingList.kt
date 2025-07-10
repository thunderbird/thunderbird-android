package net.thunderbird.feature.navigation.drawer.dropdown.ui.setting

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.common.window.WindowSizeClass
import app.k9mail.core.ui.compose.common.window.getWindowSizeInfo

@Composable
internal fun SettingList(
    modifier: Modifier = Modifier,
    content: LazyGridScope.() -> Unit,
) {
    val windowSizeInfo = getWindowSizeInfo()
    val isLandscape = windowSizeInfo.screenWidth > windowSizeInfo.screenHeight
    val useMultipleRows = isLandscape && windowSizeInfo.screenWidthSizeClass != WindowSizeClass.Compact

    val rows = if (useMultipleRows) 2 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(rows),
        modifier = modifier,
    ) {
        content()
    }
}
