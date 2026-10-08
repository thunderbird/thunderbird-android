package net.thunderbird.components.ui.bolt.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.icon
import net.thunderbird.components.ui.bolt.atom.icon.iconPath

internal val OutlinedReplyAll: ImageVector by lazy {
    icon(name = "OutlinedReplyAll", viewportWidth = 24f, viewportHeight = 24f) {
        iconPath(fill = SolidColor(Color(0xFF45474A))) {
            moveTo(x = 8.0f, y = 17.0f)
            lineToRelative(dx = -6.0f, dy = -6.0f)
            lineToRelative(dx = 6.0f, dy = -6.0f)
            lineToRelative(dx = 1.425f, dy = 1.4f)
            lineToRelative(dx = -4.6f, dy = 4.6f)
            lineToRelative(dx = 4.6f, dy = 4.6f)
            lineTo(x = 8.0f, y = 17.0f)
            close()
            moveToRelative(dx = 12.0f, dy = 2.0f)
            verticalLineToRelative(dy = -4.0f)
            curveToRelative(dx1 = 0.0f, dy1 = -0.833f, dx2 = -0.292f, dy2 = -1.542f, dx3 = -0.875f, dy3 = -2.125f)
            arcTo(
                horizontalEllipseRadius = 2.893f,
                verticalEllipseRadius = 2.893f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 17.0f,
                y1 = 12.0f,
            )
            horizontalLineToRelative(dx = -6.175f)
            lineToRelative(dx = 3.6f, dy = 3.6f)
            lineTo(x = 13.0f, y = 17.0f)
            lineToRelative(dx = -6.0f, dy = -6.0f)
            lineToRelative(dx = 6.0f, dy = -6.0f)
            lineToRelative(dx = 1.425f, dy = 1.4f)
            lineToRelative(dx = -3.6f, dy = 3.6f)
            horizontalLineTo(x = 17.0f)
            curveToRelative(dx1 = 1.383f, dy1 = 0.0f, dx2 = 2.563f, dy2 = 0.488f, dx3 = 3.538f, dy3 = 1.463f)
            curveTo(x1 = 21.512f, y1 = 12.438f, x2 = 22.0f, y2 = 13.617f, x3 = 22.0f, y3 = 15.0f)
            verticalLineToRelative(dy = 4.0f)
            horizontalLineToRelative(dx = -2.0f)
            close()
        }
    }
}

@Preview(name = "OutlinedReplyAll", showBackground = true)
@Composable
private fun OutlinedReplyAllPreview() {
    Icon(imageVector = OutlinedReplyAll)
}
