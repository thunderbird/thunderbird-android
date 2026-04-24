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
internal val OutlinedDriveFileMove: ImageVector by lazy {
    icon(
        name = "OutlinedDriveFileMove",
        viewportWidth = 960.0f,
        viewportHeight = 960.0f,
    ) {
        path(
            fill = SolidColor(Color(0xFFFFFFFF)),
        ) {
            moveTo(x = 488.0f, y = 560.0f)
            lineToRelative(dx = -65.0f, dy = 65.0f)
            lineToRelative(dx = 56.0f, dy = 56.0f)
            lineToRelative(dx = 161.0f, dy = -161.0f)
            lineToRelative(dx = -161.0f, dy = -161.0f)
            lineToRelative(dx = -56.0f, dy = 56.0f)
            lineToRelative(dx = 65.0f, dy = 65.0f)
            horizontalLineTo(x = 320.0f)
            verticalLineToRelative(dy = 80.0f)
            horizontalLineToRelative(dx = 168.0f)
            close()
            moveTo(x = 160.0f, y = 800.0f)
            quadToRelative(dx1 = -33.0f, dy1 = 0.0f, dx2 = -56.5f, dy2 = -23.5f)
            reflectiveQuadTo(x1 = 80.0f, y1 = 720.0f)
            verticalLineTo(y = 240.0f)
            quadToRelative(dx1 = 0.0f, dy1 = -33.0f, dx2 = 23.5f, dy2 = -56.5f)
            reflectiveQuadTo(x1 = 160.0f, y1 = 160.0f)
            horizontalLineToRelative(dx = 240.0f)
            lineToRelative(dx = 80.0f, dy = 80.0f)
            horizontalLineToRelative(dx = 320.0f)
            quadToRelative(dx1 = 33.0f, dy1 = 0.0f, dx2 = 56.5f, dy2 = 23.5f)
            reflectiveQuadTo(x1 = 880.0f, y1 = 320.0f)
            verticalLineToRelative(dy = 400.0f)
            quadToRelative(dx1 = 0.0f, dy1 = 33.0f, dx2 = -23.5f, dy2 = 56.5f)
            reflectiveQuadTo(x1 = 800.0f, y1 = 800.0f)
            horizontalLineTo(x = 160.0f)
            close()
            moveToRelative(dx = 0.0f, dy = -80.0f)
            horizontalLineToRelative(dx = 640.0f)
            verticalLineTo(y = 320.0f)
            horizontalLineTo(x = 447.0f)
            lineToRelative(dx = -80.0f, dy = -80.0f)
            horizontalLineTo(x = 160.0f)
            verticalLineToRelative(dy = 480.0f)
            close()
            moveToRelative(dx = 0.0f, dy = 0.0f)
            verticalLineTo(y = 240.0f)
            verticalLineToRelative(dy = 480.0f)
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IconPreview() {
    Icon(OutlinedDriveFileMove)
}
