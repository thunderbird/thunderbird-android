package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedUploadIcon: ImageVector by lazy {
    icon(
        name = "OutlinedUploadIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 440.0f, y = 640.0f)
            lineToRelative(dx = 0.0f, dy = -326.0f)
            lineTo(x = 336.0f, y = 418.0f)
            lineToRelative(dx = -56.0f, dy = -58.0f)
            lineToRelative(dx = 200.0f, dy = -200.0f)
            lineToRelative(dx = 200.0f, dy = 200.0f)
            lineToRelative(dx = -56.0f, dy = 58.0f)
            lineToRelative(dx = -104.0f, dy = -104.0f)
            lineToRelative(dx = 0.0f, dy = 326.0f)
            lineToRelative(dx = -80.0f, dy = 0.0f)
            close()
            moveTo(x = 240.0f, y = 800.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 160.0f,
                y1 = 720.0f,
            )
            lineToRelative(dx = 0.0f, dy = -120.0f)
            lineToRelative(dx = 80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 120.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -120.0f)
            lineToRelative(dx = 80.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = 120.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 720.0f,
                y1 = 800.0f,
            )
            lineTo(x = 240.0f, y = 800.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedUploadIcon, contentDescription = null)
}
