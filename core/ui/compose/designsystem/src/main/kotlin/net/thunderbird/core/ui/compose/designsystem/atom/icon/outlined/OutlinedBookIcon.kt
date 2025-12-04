package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedBookIcon: ImageVector by lazy {
    icon(
        name = "OutlinedBookIcon",
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
            lineToRelative(dx = 0.0f, dy = -640.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 240.0f,
                y1 = 80.0f,
            )
            lineToRelative(dx = 480.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 800.0f,
                y1 = 160.0f,
            )
            lineToRelative(dx = 0.0f, dy = 640.0f)
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
            lineTo(x = 240.0f, y = 880.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -640.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 280.0f)
            lineToRelative(dx = -100.0f, dy = -60.0f)
            lineToRelative(dx = -100.0f, dy = 60.0f)
            lineToRelative(dx = 0.0f, dy = -280.0f)
            lineTo(x = 240.0f, y = 160.0f)
            lineToRelative(dx = 0.0f, dy = 640.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -640.0f)
            lineToRelative(dx = 0.0f, dy = 640.0f)
            close()
            moveToRelative(dx = 200.0f, dy = -360.0f)
            lineToRelative(dx = 100.0f, dy = -60.0f)
            lineToRelative(dx = 100.0f, dy = 60.0f)
            lineToRelative(dx = -100.0f, dy = -60.0f)
            lineToRelative(dx = -100.0f, dy = 60.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedBookIcon, contentDescription = null)
}
