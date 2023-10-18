package app.k9mail.core.ui.compose.common.baseline

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp

/**
 * Adds a baseline to a Composable that typically doesn't have one.
 *
 * This can be used to align e.g. an icon to the baseline of some text next to it. See e.g. [RowScope.alignByBaseline].
 *
 * @param baseline The number of device-independent pixels (dp) the baseline is from the top of the Composable.
 */
fun Modifier.withBaseline(baseline: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val baselineInPx = baseline.roundToPx()

    layout(
        width = placeable.width,
        height = placeable.height,
        alignmentLines = mapOf(
            FirstBaseline to baselineInPx,
            LastBaseline to baselineInPx,
        ),
    ) {
        placeable.placeRelative(x = 0, y = 0)
    }
}
