package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedOpenInNewIcon: ImageVector by lazy {
    icon(name = "OutlinedOpenInNewIcon") {
        iconPath {
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
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedOpenInNewIcon, contentDescription = null)
}
