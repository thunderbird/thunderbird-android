package app.k9mail.core.ui.compose.designsystem.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.Surface
import app.k9mail.core.ui.compose.theme.K9Theme

/**
 * The [LazyColumnWithFooter] composable creates a [LazyColumn] with a footer.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param verticalArrangement The vertical arrangement of the children.
 * @param horizontalAlignment The horizontal alignment of the children.
 * @param footer The footer to be displayed at the bottom of the [LazyColumn].
 * @param content The content of the [LazyColumn].
 */
@Composable
fun LazyColumnWithFooter(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    footer: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = verticalArrangementWithFooter(verticalArrangement),
        horizontalAlignment = horizontalAlignment,
    ) {
        content()
        item { footer() }
    }
}

@Composable
private fun verticalArrangementWithFooter(verticalArrangement: Arrangement.Vertical) = remember {
    object : Arrangement.Vertical {
        override fun Density.arrange(
            totalSize: Int,
            sizes: IntArray,
            outPositions: IntArray,
        ) {
            val innerSizes = sizes.dropLast(1).toIntArray()
            val footerSize = sizes.last()
            val innerTotalSize = totalSize - footerSize

            with(verticalArrangement) {
                arrange(
                    totalSize = innerTotalSize,
                    sizes = innerSizes,
                    outPositions = outPositions,
                )
            }

            outPositions[outPositions.lastIndex] = totalSize - footerSize
        }
    }
}

@Composable
@Preview
internal fun LazyColumnWithFooterPreview() {
    K9Theme {
        Surface {
            LazyColumnWithFooter(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
                footer = { Text(text = "Footer") },
            ) {
                items(10) {
                    Text(text = "Item $it")
                }
            }
        }
    }
}
