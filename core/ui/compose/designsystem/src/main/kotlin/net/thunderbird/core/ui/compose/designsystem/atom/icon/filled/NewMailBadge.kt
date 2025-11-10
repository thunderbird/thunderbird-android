package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Suppress("MagicNumber", "UnusedReceiverParameter")
val Icons.Filled.NewMailBadge: ImageVector
    get() {
        val current = _newMailBadge
        if (current != null) return current

        return ImageVector.Builder(
            name = "net.thunderbird.core.ui.compose.theme2.MainTheme.NewMailBadge",
            defaultWidth = 12.0.dp,
            defaultHeight = 12.0.dp,
            viewportWidth = 12.0f,
            viewportHeight = 12.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFF4C430)),
                stroke = SolidColor(Color(0xFFFF8C00)),
            ) {
                moveTo(x = 6.0f, y = 0.5f)
                curveTo(x1 = 6.264f, y1 = 0.5f, x2 = 6.52061f, y2 = 0.547953f, x3 = 6.7666f, y3 = 0.643555f)
                curveTo(x1 = 7.02686f, y1 = 0.744765f, x2 = 7.25861f, y2 = 0.90322f, x3 = 7.46191f, y3 = 1.10645f)
                lineTo(x = 10.8936f, y = 4.53809f)
                curveTo(x1 = 11.0968f, y1 = 4.74139f, x2 = 11.2552f, y2 = 4.97314f, x3 = 11.3564f, y3 = 5.2334f)
                curveTo(x1 = 11.452f, y1 = 5.47939f, x2 = 11.5f, y2 = 5.736f, x3 = 11.5f, y3 = 6.0f)
                curveTo(x1 = 11.5f, y1 = 6.264f, x2 = 11.452f, y2 = 6.52061f, x3 = 11.3564f, y3 = 6.7666f)
                curveTo(x1 = 11.2552f, y1 = 7.02686f, x2 = 11.0968f, y2 = 7.25861f, x3 = 10.8936f, y3 = 7.46191f)
                lineTo(x = 7.46191f, y = 10.8936f)
                curveTo(x1 = 7.25861f, y1 = 11.0968f, x2 = 7.02686f, y2 = 11.2552f, x3 = 6.7666f, y3 = 11.3564f)
                curveTo(x1 = 6.52061f, y1 = 11.452f, x2 = 6.264f, y2 = 11.5f, x3 = 6.0f, y3 = 11.5f)
                curveTo(x1 = 5.736f, y1 = 11.5f, x2 = 5.47939f, y2 = 11.452f, x3 = 5.2334f, y3 = 11.3564f)
                curveTo(x1 = 4.97314f, y1 = 11.2552f, x2 = 4.74139f, y2 = 11.0968f, x3 = 4.53809f, y3 = 10.8936f)
                lineTo(x = 1.10645f, y = 7.46191f)
                curveTo(x1 = 0.90322f, y1 = 7.25861f, x2 = 0.744765f, y2 = 7.02686f, x3 = 0.643555f, y3 = 6.7666f)
                curveTo(x1 = 0.547953f, y1 = 6.52061f, x2 = 0.5f, y2 = 6.264f, x3 = 0.5f, y3 = 6.0f)
                curveTo(x1 = 0.5f, y1 = 5.736f, x2 = 0.547953f, y2 = 5.47939f, x3 = 0.643555f, y3 = 5.2334f)
                curveTo(x1 = 0.744766f, y1 = 4.97314f, x2 = 0.90322f, y2 = 4.74139f, x3 = 1.10645f, y3 = 4.53809f)
                lineTo(x = 4.53809f, y = 1.10645f)
                curveTo(x1 = 4.74139f, y1 = 0.90322f, x2 = 4.97314f, y2 = 0.744766f, x3 = 5.2334f, y3 = 0.643555f)
                curveTo(x1 = 5.47939f, y1 = 0.547953f, x2 = 5.736f, y2 = 0.5f, x3 = 6.0f, y3 = 0.5f)
                close()
            }
        }.build().also { _newMailBadge = it }
    }

@Suppress("ObjectPropertyName")
private var _newMailBadge: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Image(imageVector = Icons.Filled.NewMailBadge, contentDescription = null)
}
