package app.k9mail.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.icon.Icons

@Suppress("MagicNumber", "MaxLineLength", "UnusedReceiverParameter")
val Icons.Outlined.Warning: ImageVector
    get() {
        val current = _warning
        if (current != null) return current

        return ImageVector.Builder(
            name = "app.k9mail.core.ui.compose.theme2.MainTheme.Warning",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            // M40 840 L480 80 L920 840 L40 840Z M178 760 L782 760 L480 240 L178 760Z M480 720 Q497 720 508.5 708.5 Q520 697 520 680 Q520 663 508.5 651.5 Q497 640 480 640 Q463 640 451.5 651.5 Q440 663 440 680 Q440 697 451.5 708.5 Q463 720 480 720Z M440 600 L520 600 L520 400 L440 400 L440 600Z M480 500 L480 500 L480 500 L480 500Z
            path(
                fill = SolidColor(Color(0xFFFFFFFF)),
            ) {
                // M 40 840
                moveTo(x = 40.0f, y = 840.0f)
                // L 480 80
                lineTo(x = 480.0f, y = 80.0f)
                // L 920 840
                lineTo(x = 920.0f, y = 840.0f)
                // L 40 840z
                lineTo(x = 40.0f, y = 840.0f)
                close()
                // M 178 760
                moveTo(x = 178.0f, y = 760.0f)
                // L 782 760
                lineTo(x = 782.0f, y = 760.0f)
                // L 480 240
                lineTo(x = 480.0f, y = 240.0f)
                // L 178 760z
                lineTo(x = 178.0f, y = 760.0f)
                close()
                // M 480 720
                moveTo(x = 480.0f, y = 720.0f)
                // Q 497 720 508.5 708.5
                quadTo(
                    x1 = 497.0f,
                    y1 = 720.0f,
                    x2 = 508.5f,
                    y2 = 708.5f,
                )
                // Q 520 697 520 680
                quadTo(
                    x1 = 520.0f,
                    y1 = 697.0f,
                    x2 = 520.0f,
                    y2 = 680.0f,
                )
                // Q 520 663 508.5 651.5
                quadTo(
                    x1 = 520.0f,
                    y1 = 663.0f,
                    x2 = 508.5f,
                    y2 = 651.5f,
                )
                // Q 497 640 480 640
                quadTo(
                    x1 = 497.0f,
                    y1 = 640.0f,
                    x2 = 480.0f,
                    y2 = 640.0f,
                )
                // Q 463 640 451.5 651.5
                quadTo(
                    x1 = 463.0f,
                    y1 = 640.0f,
                    x2 = 451.5f,
                    y2 = 651.5f,
                )
                // Q 440 663 440 680
                quadTo(
                    x1 = 440.0f,
                    y1 = 663.0f,
                    x2 = 440.0f,
                    y2 = 680.0f,
                )
                // Q 440 697 451.5 708.5
                quadTo(
                    x1 = 440.0f,
                    y1 = 697.0f,
                    x2 = 451.5f,
                    y2 = 708.5f,
                )
                // Q 463 720 480 720z
                quadTo(
                    x1 = 463.0f,
                    y1 = 720.0f,
                    x2 = 480.0f,
                    y2 = 720.0f,
                )
                close()
                // M 440 600
                moveTo(x = 440.0f, y = 600.0f)
                // L 520 600
                lineTo(x = 520.0f, y = 600.0f)
                // L 520 400
                lineTo(x = 520.0f, y = 400.0f)
                // L 440 400
                lineTo(x = 440.0f, y = 400.0f)
                // L 440 600z
                lineTo(x = 440.0f, y = 600.0f)
                close()
                // M 480 500
                moveTo(x = 480.0f, y = 500.0f)
                // L 480 500
                lineTo(x = 480.0f, y = 500.0f)
                // L 480 500
                lineTo(x = 480.0f, y = 500.0f)
                // L 480 500z
                lineTo(x = 480.0f, y = 500.0f)
                close()
            }
        }.build().also { _warning = it }
    }

@Suppress("ObjectPropertyName")
private var _warning: ImageVector? = null
