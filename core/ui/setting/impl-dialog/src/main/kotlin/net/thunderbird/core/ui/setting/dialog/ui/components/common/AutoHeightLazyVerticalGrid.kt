package net.thunderbird.core.ui.setting.dialog.ui.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.theme2.MainTheme
import kotlin.math.ceil
import kotlinx.collections.immutable.ImmutableList

/**
 * A lazy vertical grid that automatically calculates its height to fit all items,
 * making it safe to use within other vertically scrolling layouts.
 *
 * It uses `GridCells.Adaptive` and calculates the number of rows based on the available width and the
 * provided [itemSize].
 *
 * @param items The list of items to display in the grid.
 * @param itemSize The fixed size for each item in the grid.
 * @param modifier The modifier to be applied to the grid container.
 * @param itemContent The composable content to be displayed for each item.
 */
@Composable
internal fun <T> AutoHeightLazyVerticalGrid(
    items: ImmutableList<T>,
    itemSize: Dp,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    val state = rememberLazyGridState()

    BoxWithConstraints(
        modifier = modifier,
    ) {
        val containerWidth = this.maxWidth
        val columnCount = (containerWidth / itemSize).toInt().coerceAtLeast(1)
        val rowCount = ceil(items.size.toFloat() / columnCount).toInt()
        val gridHeight = (itemSize * rowCount) +
            (MainTheme.spacings.default * (rowCount - 1)).coerceAtLeast(0.dp)

        LazyVerticalGrid(
            state = state,
            columns = GridCells.Adaptive(minSize = MainTheme.sizes.iconAvatar),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            horizontalArrangement = Arrangement.spacedBy(MainTheme.spacings.default),
            modifier = Modifier.height(gridHeight),
            userScrollEnabled = false,
        ) {
            items(items) { item ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    itemContent(item)
                }
            }
        }
    }
}
