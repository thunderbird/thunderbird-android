package net.thunderbird.feature.notification.api.ui.icon.atom

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("MagicNumber", "MaxLineLength")
internal val Warning: ImageVector
    get() {
        val current = _warning
        if (current != null) return current

        return ImageVector.Builder(
            name = "net.thunderbird.feature.notification.api.ui.icon.atom.Warning",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
            ) {
                moveTo(x = 40.0f, y = 840.0f)
                lineTo(x = 480.0f, y = 80.0f)
                lineTo(x = 920.0f, y = 840.0f)
                lineTo(x = 40.0f, y = 840.0f)
                close()
                moveTo(x = 178.0f, y = 760.0f)
                lineTo(x = 782.0f, y = 760.0f)
                lineTo(x = 480.0f, y = 240.0f)
                lineTo(x = 178.0f, y = 760.0f)
                close()
                moveTo(x = 480.0f, y = 720.0f)
                quadTo(x1 = 497.0f, y1 = 720.0f, x2 = 508.5f, y2 = 708.5f)
                quadTo(x1 = 520.0f, y1 = 697.0f, x2 = 520.0f, y2 = 680.0f)
                quadTo(x1 = 520.0f, y1 = 663.0f, x2 = 508.5f, y2 = 651.5f)
                quadTo(x1 = 497.0f, y1 = 640.0f, x2 = 480.0f, y2 = 640.0f)
                quadTo(x1 = 463.0f, y1 = 640.0f, x2 = 451.5f, y2 = 651.5f)
                quadTo(x1 = 440.0f, y1 = 663.0f, x2 = 440.0f, y2 = 680.0f)
                quadTo(x1 = 440.0f, y1 = 697.0f, x2 = 451.5f, y2 = 708.5f)
                quadTo(x1 = 463.0f, y1 = 720.0f, x2 = 480.0f, y2 = 720.0f)
                close()
                moveTo(x = 440.0f, y = 600.0f)
                lineTo(x = 520.0f, y = 600.0f)
                lineTo(x = 520.0f, y = 400.0f)
                lineTo(x = 440.0f, y = 400.0f)
                lineTo(x = 440.0f, y = 600.0f)
                close()
                moveTo(x = 480.0f, y = 500.0f)
                lineTo(x = 480.0f, y = 500.0f)
                lineTo(x = 480.0f, y = 500.0f)
                lineTo(x = 480.0f, y = 500.0f)
                close()
            }
        }.build().also { _warning = it }
    }

@Suppress("ObjectPropertyName")
private var _warning: ImageVector? = null
