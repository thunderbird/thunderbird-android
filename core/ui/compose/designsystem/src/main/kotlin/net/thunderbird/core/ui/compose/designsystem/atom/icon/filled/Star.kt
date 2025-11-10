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
val Icons.Filled.Star: ImageVector
    get() {
        val current = _star
        if (current != null) return current

        return ImageVector.Builder(
            name = "net.thunderbird.core.ui.compose.theme2.MainTheme.Star",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFF8C00)),
            ) {
                moveTo(x = 12.0f, y = 17.77f)
                lineTo(x = 18.18f, y = 21.5f)
                lineTo(x = 16.54f, y = 14.47f)
                lineTo(x = 22.0f, y = 9.74f)
                lineTo(x = 14.81f, y = 9.13f)
                lineTo(x = 12.0f, y = 2.5f)
                lineTo(x = 9.19f, y = 9.13f)
                lineTo(x = 2.0f, y = 9.74f)
                lineTo(x = 7.46f, y = 14.47f)
                lineTo(x = 5.82f, y = 21.5f)
                lineTo(x = 12.0f, y = 17.77f)
                close()
            }
        }.build().also { _star = it }
    }

@Suppress("ObjectPropertyName")
private var _star: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Image(imageVector = Icons.Filled.Star, contentDescription = null)
}
