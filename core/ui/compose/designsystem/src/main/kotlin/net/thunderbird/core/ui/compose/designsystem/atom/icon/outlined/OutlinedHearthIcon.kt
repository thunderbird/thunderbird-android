package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

@Suppress("MagicNumber")
internal val OutlinedHearthIcon: ImageVector by lazy {
    icon(
        name = "OutlinedHearthIcon",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        iconPath {
            moveTo(x = 480.0f, y = 840.0f)
            lineToRelative(dx = -58.0f, dy = -52.0f)
            quadToRelative(
                dx1 = -101.0f,
                dy1 = -91.0f,
                dx2 = -167.0f,
                dy2 = -157.0f,
            )
            reflectiveQuadTo(
                x1 = 150.0f,
                y1 = 512.5f,
            )
            quadTo(
                x1 = 111.0f,
                y1 = 460.0f,
                x2 = 95.5f,
                y2 = 416.0f,
            )
            reflectiveQuadTo(
                x1 = 80.0f,
                y1 = 326.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -94.0f,
                dx2 = 63.0f,
                dy2 = -157.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 157.0f,
                dy1 = -63.0f,
            )
            quadToRelative(
                dx1 = 52.0f,
                dy1 = 0.0f,
                dx2 = 99.0f,
                dy2 = 22.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 81.0f,
                dy1 = 62.0f,
            )
            quadToRelative(
                dx1 = 34.0f,
                dy1 = -40.0f,
                dx2 = 81.0f,
                dy2 = -62.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 99.0f,
                dy1 = -22.0f,
            )
            quadToRelative(
                dx1 = 94.0f,
                dy1 = 0.0f,
                dx2 = 157.0f,
                dy2 = 63.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 63.0f,
                dy1 = 157.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 46.0f,
                dx2 = -15.5f,
                dy2 = 90.0f,
            )
            reflectiveQuadTo(
                x1 = 810.0f,
                y1 = 512.5f,
            )
            quadTo(
                x1 = 771.0f,
                y1 = 565.0f,
                x2 = 705.0f,
                y2 = 631.0f,
            )
            reflectiveQuadTo(
                x1 = 538.0f,
                y1 = 788.0f,
            )
            lineToRelative(dx = -58.0f, dy = 52.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -108.0f)
            quadToRelative(
                dx1 = 96.0f,
                dy1 = -86.0f,
                dx2 = 158.0f,
                dy2 = -147.5f,
            )
            reflectiveQuadToRelative(
                dx1 = 98.0f,
                dy1 = -107.0f,
            )
            quadToRelative(
                dx1 = 36.0f,
                dy1 = -45.5f,
                dx2 = 50.0f,
                dy2 = -81.0f,
            )
            reflectiveQuadToRelative(
                dx1 = 14.0f,
                dy1 = -70.5f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = -60.0f,
                dx2 = -40.0f,
                dy2 = -100.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -100.0f,
                dy1 = -40.0f,
            )
            quadToRelative(
                dx1 = -47.0f,
                dy1 = 0.0f,
                dx2 = -87.0f,
                dy2 = 26.5f,
            )
            reflectiveQuadTo(
                x1 = 518.0f,
                y1 = 280.0f,
            )
            lineToRelative(dx = -76.0f, dy = 0.0f)
            quadToRelative(
                dx1 = -15.0f,
                dy1 = -41.0f,
                dx2 = -55.0f,
                dy2 = -67.5f,
            )
            reflectiveQuadTo(
                x1 = 300.0f,
                y1 = 186.0f,
            )
            quadToRelative(
                dx1 = -60.0f,
                dy1 = 0.0f,
                dx2 = -100.0f,
                dy2 = 40.0f,
            )
            reflectiveQuadToRelative(
                dx1 = -40.0f,
                dy1 = 100.0f,
            )
            quadToRelative(
                dx1 = 0.0f,
                dy1 = 35.0f,
                dx2 = 14.0f,
                dy2 = 70.5f,
            )
            reflectiveQuadToRelative(
                dx1 = 50.0f,
                dy1 = 81.0f,
            )
            quadToRelative(
                dx1 = 36.0f,
                dy1 = 45.5f,
                dx2 = 98.0f,
                dy2 = 107.0f,
            )
            reflectiveQuadTo(
                x1 = 480.0f,
                y1 = 732.0f,
            )
            close()
            moveToRelative(dx = 0.0f, dy = -273.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    Icon(imageVector = OutlinedHearthIcon, contentDescription = null)
}
