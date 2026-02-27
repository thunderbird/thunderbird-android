package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedDescriptionIcon: ImageVector by lazy {
    icon(
        name = "OutlinedDescriptionIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 320.0f, y = 720.0f)
            lineToRelative(dx = 320.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            lineTo(x = 320.0f, y = 640.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -160.0f)
            lineToRelative(dx = 320.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            lineTo(x = 320.0f, y = 400.0f)
            close()
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
            lineToRelative(dx = 320.0f, dy = 0.0f)
            lineToRelative(dx = 240.0f, dy = 240.0f)
            lineToRelative(dx = 0.0f, dy = 480.0f)
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
            moveToRelative(dx = 280.0f, dy = -520.0f)
            lineToRelative(dx = 0.0f, dy = -200.0f)
            lineTo(x = 240.0f, y = 160.0f)
            lineToRelative(dx = 0.0f, dy = 640.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -440.0f)
            close()
            moveTo(x = 240.0f, y = 160.0f)
            lineToRelative(dx = 0.0f, dy = 200.0f)
            close()
            lineToRelative(dx = 0.0f, dy = 640.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedDescriptionIcon, contentDescription = null)
}
