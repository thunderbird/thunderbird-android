package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density

/**
 * The [LazyColumnWithHeaderFooter] composable creates a [LazyColumn] with header and footer items.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param horizontalAlignment The horizontal alignment of the children.
 * @param header The header to be displayed at the top of the [LazyColumn].
 * @param footer The footer to be displayed at the bottom of the [LazyColumn].
 * @param content The content of the [LazyColumn].
 */
@Composable
fun LazyColumnWithHeaderFooter(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        verticalArrangement = verticalArrangementWithHeaderFooter(verticalArrangement),
        horizontalAlignment = horizontalAlignment,
    ) {
        item { header() }
        content()
        item { footer() }
    }
}

@Composable
private fun verticalArrangementWithHeaderFooter(verticalArrangement: Arrangement.Vertical) = remember {
    object : Arrangement.Vertical {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray,
        ) {
            val headerSize = sizes.first()
            val footerSize = sizes.last()
            val innerTotalSize = totalSize - (headerSize + footerSize)
            val innerSizes = sizes.copyOfRange(1, sizes.lastIndex)
            val innerOutPositions = outPositions.copyOfRange(1, outPositions.lastIndex)

            with(verticalArrangement) {
                arrange(
                    totalSize = innerTotalSize,
                    sizes = innerSizes,
                    outPositions = innerOutPositions,
                )
            }

            innerOutPositions.forEachIndexed { index, position -> outPositions[index + 1] = position + headerSize }
            outPositions[0] = 0
            outPositions[outPositions.lastIndex] = totalSize - footerSize
        }
    }
}
