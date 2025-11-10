package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

@Suppress("MagicNumber", "UnusedReceiverParameter")
internal val Icons.Outlined.OpenInNew: ImageVector
    get() {
        val current = _openInNew
        if (current != null) return current

        return ImageVector.Builder(
            name = "app.k9mail.core.ui.compose.theme2.MainTheme.OpenInNew",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
            ) {
                moveTo(x = 5.0f, y = 21.0f)
                curveTo(x1 = 4.45f, y1 = 21.0f, x2 = 3.97917f, y2 = 20.8042f, x3 = 3.5875f, y3 = 20.4125f)
                curveTo(x1 = 3.19583f, y1 = 20.0208f, x2 = 3.0f, y2 = 19.55f, x3 = 3.0f, y3 = 19.0f)
                verticalLineTo(y = 5.0f)
                curveTo(x1 = 3.0f, y1 = 4.45f, x2 = 3.19583f, y2 = 3.97917f, x3 = 3.5875f, y3 = 3.5875f)
                curveTo(x1 = 3.97917f, y1 = 3.19583f, x2 = 4.45f, y2 = 3.0f, x3 = 5.0f, y3 = 3.0f)
                horizontalLineTo(x = 12.0f)
                verticalLineTo(y = 5.0f)
                horizontalLineTo(x = 5.0f)
                verticalLineTo(y = 19.0f)
                horizontalLineTo(x = 19.0f)
                verticalLineTo(y = 12.0f)
                horizontalLineTo(x = 21.0f)
                verticalLineTo(y = 19.0f)
                curveTo(x1 = 21.0f, y1 = 19.55f, x2 = 20.8042f, y2 = 20.0208f, x3 = 20.4125f, y3 = 20.4125f)
                curveTo(x1 = 20.0208f, y1 = 20.8042f, x2 = 19.55f, y2 = 21.0f, x3 = 19.0f, y3 = 21.0f)
                horizontalLineTo(x = 5.0f)
                close()
                moveTo(x = 9.7f, y = 15.7f)
                lineTo(x = 8.3f, y = 14.3f)
                lineTo(x = 17.6f, y = 5.0f)
                horizontalLineTo(x = 14.0f)
                verticalLineTo(y = 3.0f)
                horizontalLineTo(x = 21.0f)
                verticalLineTo(y = 10.0f)
                horizontalLineTo(x = 19.0f)
                verticalLineTo(y = 6.4f)
                lineTo(x = 9.7f, y = 15.7f)
                close()
            }
        }.build().also { _openInNew = it }
    }

@Suppress("ObjectPropertyName")
private var _openInNew: ImageVector? = null
