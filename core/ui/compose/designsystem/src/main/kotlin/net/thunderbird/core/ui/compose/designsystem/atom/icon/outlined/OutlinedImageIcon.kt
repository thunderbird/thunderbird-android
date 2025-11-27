package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedImageIcon: ImageVector by lazy {
    icon(
        name = "OutlinedImageIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 200.0f, y = 840.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 120.0f,
                y1 = 760.0f,
            )
            lineToRelative(dx = 0.0f, dy = -560.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 200.0f,
                y1 = 120.0f,
            )
            lineToRelative(dx = 560.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 840.0f,
                y1 = 200.0f,
            )
            lineToRelative(dx = 0.0f, dy = 560.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 760.0f,
                y1 = 840.0f,
            )
            lineTo(x = 200.0f, y = 840.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 560.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -560.0f)
            lineTo(x = 200.0f, y = 200.0f)
            lineToRelative(dx = 0.0f, dy = 560.0f)
            close()
            moveToRelative(dx = 40.0f, dy = -80.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineTo(x = 570.0f, y = 480.0f)
            lineTo(x = 450.0f, y = 640.0f)
            lineToRelative(dx = -90.0f, dy = -120.0f)
            lineToRelative(dx = -120.0f, dy = 160.0f)
            close()
            moveToRelative(dx = -40.0f, dy = 80.0f)
            lineToRelative(dx = 0.0f, dy = -560.0f)
            lineToRelative(dx = 0.0f, dy = 560.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedImageIcon, contentDescription = null)
}
