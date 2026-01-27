package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedFavoriteFolderIcon: ImageVector by lazy {
    icon(
        name = "OutlinedFavoriteFolderIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 504.0f, y = 668.0f)
            lineToRelative(dx = 92.0f, dy = -70.0f)
            lineToRelative(dx = 92.0f, dy = 70.0f)
            lineToRelative(dx = -34.0f, dy = -114.0f)
            lineToRelative(dx = 92.0f, dy = -74.0f)
            lineTo(x = 632.0f, y = 480.0f)
            lineToRelative(dx = -36.0f, dy = -112.0f)
            lineToRelative(dx = -36.0f, dy = 112.0f)
            lineTo(x = 446.0f, y = 480.0f)
            lineToRelative(dx = 92.0f, dy = 74.0f)
            lineToRelative(dx = -34.0f, dy = 114.0f)
            close()
            moveTo(x = 160.0f, y = 800.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 80.0f,
                y1 = 720.0f,
            )
            lineToRelative(dx = 0.0f, dy = -480.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 160.0f,
                y1 = 160.0f,
            )
            lineToRelative(dx = 240.0f, dy = 0.0f)
            lineToRelative(dx = 80.0f, dy = 80.0f)
            lineToRelative(dx = 320.0f, dy = 0.0f)
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
            lineToRelative(dx = 0.0f, dy = 400.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 800.0f,
                y1 = 800.0f,
            )
            lineTo(x = 160.0f, y = 800.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            lineToRelative(dx = 640.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -400.0f)
            lineTo(x = 447.0f, y = 320.0f)
            lineToRelative(dx = -80.0f, dy = -80.0f)
            lineTo(x = 160.0f, y = 240.0f)
            lineToRelative(dx = 0.0f, dy = 480.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -480.0f)
            lineToRelative(dx = 0.0f, dy = 480.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedFavoriteFolderIcon, contentDescription = null)
}
