package net.thunderbird.core.ui.compose.designsystem.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon

@Suppress("MagicNumber")
internal val OutlinedMarkEmailUnread: ImageVector by lazy {
    icon(
        name = "OutlinedMarkEmailUnread",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        path(
            fill = SolidColor(Color(0xFFFFFFFF)),
        ) {
            moveTo(x = 160.0f, y = 800.0f)
            quadToRelative(dx1 = -33.0f, dy1 = 0.0f, dx2 = -56.5f, dy2 = -23.5f)
            reflectiveQuadTo(x1 = 80.0f, y1 = 720.0f)
            verticalLineTo(y = 240.0f)
            quadToRelative(dx1 = 0.0f, dy1 = -33.0f, dx2 = 23.5f, dy2 = -56.5f)
            reflectiveQuadTo(x1 = 160.0f, y1 = 160.0f)
            horizontalLineToRelative(dx = 404.0f)
            quadToRelative(dx1 = -4.0f, dy1 = 20.0f, dx2 = -4.0f, dy2 = 40.0f)
            reflectiveQuadToRelative(dx1 = 4.0f, dy1 = 40.0f)
            horizontalLineTo(x = 160.0f)
            lineToRelative(dx = 320.0f, dy = 200.0f)
            lineToRelative(dx = 146.0f, dy = -91.0f)
            quadToRelative(dx1 = 14.0f, dy1 = 13.0f, dx2 = 30.5f, dy2 = 22.5f)
            reflectiveQuadTo(x1 = 691.0f, y1 = 388.0f)
            lineTo(x = 480.0f, y = 520.0f)
            lineTo(x = 160.0f, y = 320.0f)
            verticalLineToRelative(dy = 400.0f)
            horizontalLineToRelative(dx = 640.0f)
            verticalLineTo(y = 396.0f)
            quadToRelative(dx1 = 23.0f, dy1 = -5.0f, dx2 = 43.0f, dy2 = -14.0f)
            reflectiveQuadToRelative(dx1 = 37.0f, dy1 = -22.0f)
            verticalLineToRelative(dy = 360.0f)
            quadToRelative(dx1 = 0.0f, dy1 = 33.0f, dx2 = -23.5f, dy2 = 56.5f)
            reflectiveQuadTo(x1 = 800.0f, y1 = 800.0f)
            horizontalLineTo(x = 160.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -560.0f)
            verticalLineToRelative(dy = 480.0f)
            verticalLineToRelative(dy = -480.0f)
            close()
            moveToRelative(dx = 600.0f, dy = 80.0f)
            quadToRelative(dx1 = -50.0f, dy1 = 0.0f, dx2 = -85.0f, dy2 = -35.0f)
            reflectiveQuadToRelative(dx1 = -35.0f, dy1 = -85.0f)
            quadToRelative(dx1 = 0.0f, dy1 = -50.0f, dx2 = 35.0f, dy2 = -85.0f)
            reflectiveQuadToRelative(dx1 = 85.0f, dy1 = -35.0f)
            quadToRelative(dx1 = 50.0f, dy1 = 0.0f, dx2 = 85.0f, dy2 = 35.0f)
            reflectiveQuadToRelative(dx1 = 35.0f, dy1 = 85.0f)
            quadToRelative(dx1 = 0.0f, dy1 = 50.0f, dx2 = -35.0f, dy2 = 85.0f)
            reflectiveQuadToRelative(dx1 = -85.0f, dy1 = 35.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPreview() {
    Icon(imageVector = OutlinedMarkEmailUnread, contentDescription = null)
}
