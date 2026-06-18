package net.thunderbird.core.ui.compose.designsystem.atom.chip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ColorChip(
    modifier: Modifier = Modifier,
    height: Dp = ColorChipDefaults.chipDefaultHeight,
    width: Dp = ColorChipDefaults.chipDefaultWidth,
    color: Color = ColorChipDefaults.chipDefaultColor,
) {
    val brush = SolidColor(color)
    Canvas(
        modifier = modifier.size(height = height, width = width),
        onDraw = {
            drawRoundRect(
                brush = brush,
                cornerRadius = CornerRadius(width.value, width.value),
            )
        },
    )
}

@Preview
@Composable
private fun PreviewDefault() {
    ColorChip()
}

@Preview
@Composable
private fun PreviewMagenta() {
    ColorChip(
        color = Color.Magenta,
    )
}

@Preview
@Composable
private fun PreviewBigRed() {
    ColorChip(
        height = 30.dp,
        width = 6.dp,
        color = Color.Red,
    )
}

private object ColorChipDefaults {
    val chipDefaultHeight = 20.dp
    val chipDefaultWidth = 3.dp
    val chipDefaultColor = Color.Blue
}
