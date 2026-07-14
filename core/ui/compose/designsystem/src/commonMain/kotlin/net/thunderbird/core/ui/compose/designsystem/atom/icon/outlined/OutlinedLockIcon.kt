package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedLockIcon: ImageVector by lazy {
    icon(
        name = "OutlinedLockIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 240.0f, y = 880.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 160.0f,
                y1 = 800.0f,
            )
            lineToRelative(dx = 0.0f, dy = -400.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 240.0f,
                y1 = 320.0f,
            )
            lineToRelative(dx = 40.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -83.0f,
                dx2 = 58.5f,
                dy2 = -141.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 40.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 141.5f,
                dy1 = 58.5f,
            )
            reflectiveQuadTo(
                x1 = 680.0f,
                y1 = 240.0f,
            )
            lineToRelative(dx = 0.0f, dy = 80.0f)
            lineToRelative(dx = 40.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 800.0f,
                y1 = 400.0f,
            )
            lineToRelative(dx = 0.0f, dy = 400.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 720.0f,
                y1 = 880.0f,
            )
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -400.0f)
            lineTo(x = 240.0f, y = 400.0f)
            close()
            moveToRelative(dx = 296.5f, dy = -143.5f)
            quadTo(
                x1 = 560.0f,
                y1 = 633.0f,
                x2 = 560.0f,
                y2 = 600.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -23.5f,
                dy1 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 520.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -56.5f,
                dy1 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 400.0f,
                y1 = 600.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 23.5f,
                dy1 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 680.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 56.5f,
                dy1 = -23.5f,
            )
            moveTo(x = 360.0f, y = 320.0f)
            lineToRelative(dx = 240.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -50.0f,
                dx2 = -35.0f,
                dy2 = -85.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -85.0f,
                dy1 = -35.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -85.0f,
                dy1 = 35.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -35.0f,
                dy1 = 85.0f,
            )
            close()
            moveTo(x = 240.0f, y = 800.0f)
            lineToRelative(dx = 0.0f, dy = -400.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedLockIcon, contentDescription = null)
}
