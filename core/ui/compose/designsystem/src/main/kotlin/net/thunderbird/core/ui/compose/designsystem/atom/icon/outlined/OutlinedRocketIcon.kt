package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedRocketIcon: ImageVector by lazy {
    icon(
        name = "OutlinedRocketIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 240.0f, y = 762.0f)
            lineToRelative(dx = 79.0f, dy = -32.0f)
            quadToRelative(
                dx1 = -10.0f,
                dy1 = -29.0f,
                dx2 = -18.5f,
                dy2 = -59.0f,
            )
            reflectiveQuadTo(
                x1 = 287.0f,
                y1 = 611.0f,
            )
            lineToRelative(dx = -47.0f, dy = 32.0f)
            lineToRelative(dx = 0.0f, dy = 119.0f)
            close()
            moveToRelative(dx = 160.0f, dy = -42.0f)
            lineToRelative(dx = 160.0f, dy = 0.0f)
            quadToRelative(
                dx1 = 18.0f,
                dy1 = -40.0f,
                dx2 = 29.0f,
                dy2 = -97.5f,
            )
            reflectiveQuadTo(
                x1 = 600.0f,
                y1 = 505.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -99.0f,
                dx2 = -33.0f,
                dy2 = -187.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 181.0f,
            )
            quadToRelative(
                dx1 = -54.0f,
                dy1 = 48.0f,
                dx2 = -87.0f,
                dy2 = 136.5f,
            )
            reflectiveQuadTo(
                x1 = 360.0f,
                y1 = 505.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 60.0f,
                dx2 = 11.0f,
                dy2 = 117.5f,
            )
            reflectiveQuadToRelative(
                dx1 = 29.0f,
                dy1 = 97.5f,
            )
            close()
            moveToRelative(dx = 80.0f, dy = -200.0f)
            quadToRelative(
                dx1 = -33.0f,
                dy1 = 0.0f,
                dx2 = -56.5f,
                dy2 = -23.5f,
            )
            reflectiveQuadTo(
                x1 = 400.0f,
                y1 = 440.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -33.0f,
                dx2 = 23.5f,
                dy2 = -56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 360.0f,
            )
            quadToRelative(
                dx1 = 33.0f,
                dy1 = 0.0f,
                dx2 = 56.5f,
                dy2 = 23.5f,
            )
            reflectiveQuadTo(
                x1 = 560.0f,
                y1 = 440.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 33.0f,
                dx2 = -23.5f,
                dy2 = 56.5f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 520.0f,
            )
            close()
            moveToRelative(dx = 240.0f, dy = 242.0f)
            lineToRelative(dx = 0.0f, dy = -119.0f)
            lineToRelative(dx = -47.0f, dy = -32.0f)
            quadToRelative(
                dx1 = -5.0f,
                dy1 = 30.0f,
                dx2 = -13.5f,
                dy2 = 60.0f,
            )
            reflectiveQuadTo(
                x1 = 641.0f,
                y1 = 730.0f,
            )
            lineToRelative(dx = 79.0f, dy = 32.0f)
            close()
            moveTo(x = 480.0f, y = 79.0f)
            quadToRelative(
                dx1 = 99.0f,
                dy1 = 72.0f,
                dx2 = 149.5f,
                dy2 = 183.0f,
            )
            reflectiveQuadTo(
                x1 = 680.0f,
                y1 = 520.0f,
            )
            lineToRelative(dx = 84.0f, dy = 56.0f)
            quadToRelative(
                dx1 = 17.0f,
                dy1 = 11.0f,
                dx2 = 26.5f,
                dy2 = 29.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 9.5f,
                dy1 = 38.0f,
            )
            lineToRelative(dx = 0.0f, dy = 237.0f)
            lineToRelative(dx = -199.0f, dy = -80.0f)
            lineTo(x = 359.0f, y = 800.0f)
            lineTo(x = 160.0f, y = 880.0f)
            lineToRelative(dx = 0.0f, dy = -237.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -20.0f,
                dx2 = 9.5f,
                dy2 = -38.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 26.5f,
                dy1 = -29.0f,
            )
            lineToRelative(dx = 84.0f, dy = -56.0f)
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -147.0f,
                dx2 = 50.5f,
                dy2 = -258.0f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 79.0f,
            )
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedRocketIcon, contentDescription = null)
}
