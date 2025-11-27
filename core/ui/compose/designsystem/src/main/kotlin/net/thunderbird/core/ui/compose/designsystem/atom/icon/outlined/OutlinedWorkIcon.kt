package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedWorkIcon: ImageVector by lazy {
    icon(
        name = "OutlinedWorkIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 160.0f, y = 840.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 80.0f,
                y1 = 760.0f,
            )
            lineToRelative(dx = 0.0f, dy = -440.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 160.0f,
                y1 = 240.0f,
            )
            lineToRelative(dx = 160.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 400.0f,
                y1 = 80.0f,
            )
            lineToRelative(dx = 160.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 640.0f,
                y1 = 160.0f,
            )
            lineToRelative(dx = 0.0f, dy = 80.0f)
            lineToRelative(dx = 160.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 880.0f,
                y1 = 320.0f,
            )
            lineToRelative(dx = 0.0f, dy = 440.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 800.0f,
                y1 = 840.0f,
            )
            lineTo(x = 160.0f, y = 840.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 640.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -440.0f)
            lineTo(x = 160.0f, y = 320.0f)
            lineToRelative(dx = 0.0f, dy = 440.0f)
            close()
            moveToRelative(dx = 240.0f, dy = -520.0f)
            lineToRelative(dx = 160.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -80.0f)
            lineTo(x = 400.0f, y = 160.0f)
            lineToRelative(dx = 0.0f, dy = 80.0f)
            close()
            moveTo(x = 160.0f, y = 760.0f)
            lineToRelative(dx = 0.0f, dy = -440.0f)
            lineToRelative(dx = 0.0f, dy = 440.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedWorkIcon, contentDescription = null)
}
