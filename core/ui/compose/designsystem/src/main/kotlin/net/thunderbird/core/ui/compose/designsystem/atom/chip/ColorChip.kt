package net.thunderbird.core.ui.compose.designsystem.atom.chip

import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ColorChip(
    modifier: Modifier = Modifier,
    height: Dp = ColorChip.chipDefaultHeight,
    width: Dp = ColorChip.chipDefaultWidth,
    color: Int = Color.BLUE,
) {
    val brush = SolidColor(androidx.compose.ui.graphics.Color(color.toLong()))
    Canvas(
        modifier = modifier.size(height = height, width = width),
        onDraw = {
            drawRoundRect(
                brush = brush,
                cornerRadius = CornerRadius(3.0f, 3.0f),
            )
        }
    )
}

@Preview
@Composable
private fun Preview() {
    ColorChip()
}

object ColorChip {
    val chipDefaultHeight = 20.dp
    val chipDefaultWidth = 3.dp
}
