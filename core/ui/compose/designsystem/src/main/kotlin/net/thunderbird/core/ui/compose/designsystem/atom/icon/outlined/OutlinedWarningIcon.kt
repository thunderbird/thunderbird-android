package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedWarningIcon: ImageVector by lazy {
    icon(
        name = "OutlinedWarningIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
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
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedWarningIcon, contentDescription = null)
}
