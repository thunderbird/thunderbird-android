package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedPersonIcon: ImageVector by lazy {
    icon(
        name = "OutlinedPersonIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 480.0f, y = 480.0f)
            quadToRelative(
                dx1 = -66.0f,
                dy1 = 0.0f,
                dx2 = -113.0f,
                dy2 = -47.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -47.0f,
                dy1 = -113.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -66.0f,
                dx2 = 47.0f,
                dy2 = -113.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 113.0f,
                dy1 = -47.0f,
            )
            quadToRelative(
                dx1 = 66.0f,
                dy1 = 0.0f,
                dx2 = 113.0f,
                dy2 = 47.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 47.0f,
                dy1 = 113.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 66.0f,
                dx2 = -47.0f,
                dy2 = 113.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -113.0f,
                dy1 = 47.0f,
            )
            close()
            moveTo(x = 160.0f, y = 800.0f)
            lineToRelative(dx = 0.0f, dy = -112.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -34.0f,
                dx2 = 17.5f,
                dy2 = -62.5f,
            )
            reflectiveQuadTo(
                x1 = 224.0f,
                y1 = 582.0f,
            )
            quadToRelative(
                dx1 = 62.0f,
                dy1 = -31.0f,
                dx2 = 126.0f,
                dy2 = -46.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 520.0f,
            )
            quadToRelative(
                dx1 = 66.0f,
                dy1 = 0.0f,
                dx2 = 130.0f,
                dy2 = 15.5f,
            )
            reflectiveQuadTo(
                x1 = 736.0f,
                y1 = 582.0f,
            )
            quadToRelative(
                dx1 = 29.0f,
                dy1 = 15.0f,
                dx2 = 46.5f,
                dy2 = 43.5f,
            )
            reflectiveQuadTo(
                x1 = 800.0f,
                y1 = 688.0f,
            )
            lineToRelative(dx = 0.0f, dy = 112.0f)
            lineTo(x = 160.0f, y = 800.0f)
            close()
            moveToRelative(dx = 80.0f, dy = -80.0f)
            lineToRelative(dx = 480.0f, dy = 0.0f)
            lineToRelative(dx = 0.0f, dy = -32.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -11.0f,
                dx2 = -5.5f,
                dy2 = -20.0f,
            )
            reflectiveQuadTo(
                x1 = 700.0f,
                y1 = 654.0f,
            )
            quadToRelative(
                dx1 = -54.0f,
                dy1 = -27.0f,
                dx2 = -109.0f,
                dy2 = -40.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 600.0f,
            )
            quadToRelative(
                dx1 = -56.0f,
                dy1 = 0.0f,
                dx2 = -111.0f,
                dy2 = 13.5f,
            )
            reflectiveQuadTo(
                x1 = 260.0f,
                y1 = 654.0f,
            )
            quadToRelative(
                dx1 = -9.0f,
                dy1 = 5.0f,
                dx2 = -14.5f,
                dy2 = 14.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -5.5f,
                dy1 = 20.0f,
            )
            lineToRelative(dx = 0.0f, dy = 32.0f)
            close()
            moveToRelative(dx = 240.0f, dy = -320.0f)
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 560.0f,
                y1 = 320.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = -23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 240.0f,
            )
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 400.0f,
                y1 = 320.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = 23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 400.0f,
            )
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 400.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedPersonIcon, contentDescription = null)
}
