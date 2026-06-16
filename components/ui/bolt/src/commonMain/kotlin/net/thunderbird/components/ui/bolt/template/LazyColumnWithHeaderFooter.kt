package net.thunderbird.components.ui.bolt.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import net.thunderbird.components.ui.bolt.PreviewWithTheme
import net.thunderbird.components.ui.bolt.atom.Surface
import net.thunderbird.components.ui.bolt.atom.text.TextBodyLarge
import net.thunderbird.components.ui.bolt.atom.text.TextTitleMedium
import net.thunderbird.components.ui.bolt.theme.BoltTheme

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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    header: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
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

@Composable
@Preview(showBackground = true)
internal fun LazyColumnWithHeaderFooterPreview() {
    PreviewWithTheme {
        Surface {
            LazyColumnWithHeaderFooter(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BoltTheme.spacings.double, Alignment.CenterVertically),
                header = { TextTitleMedium(text = "Header") },
                footer = { TextTitleMedium(text = "Footer") },
            ) {
                items(10) {
                    TextBodyLarge(text = "Item $it")
                }
            }
        }
    }
}
