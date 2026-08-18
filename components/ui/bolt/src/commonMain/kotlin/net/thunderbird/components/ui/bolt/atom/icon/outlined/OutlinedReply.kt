package net.thunderbird.components.ui.bolt.atom.icon.outlined

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.components.ui.bolt.atom.icon.Icon
import net.thunderbird.components.ui.bolt.atom.icon.icon
import net.thunderbird.components.ui.bolt.atom.icon.iconPath

internal val OutlinedReply: ImageVector by lazy {
    icon(name = "OutlinedReply", viewportWidth = 24f, viewportHeight = 24f) {
        iconPath(fill = SolidColor(Color(0xFF45474A))) {
            moveTo(x = 19.0f, y = 19.0f)
            verticalLineToRelative(dy = -4.0f)
            arcToRelative(
                a = 3.0f,
                b = 3.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = -0.87f,
                dy1 = -2.12f,
            )
            arcTo(
                horizontalEllipseRadius = 3.0f,
                verticalEllipseRadius = 3.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 16.0f,
                y1 = 12.0f,
            )
            horizontalLineTo(x = 6.82f)
            lineToRelative(dx = 3.6f, dy = 3.6f)
            lineTo(x = 9.0f, y = 17.0f)
            lineToRelative(dx = -6.0f, dy = -6.0f)
            lineToRelative(dx = 6.0f, dy = -6.0f)
            lineToRelative(dx = 1.43f, dy = 1.4f)
            lineToRelative(dx = -3.6f, dy = 3.6f)
            horizontalLineTo(x = 16.0f)
            arcToRelative(
                a = 4.8f,
                b = 4.8f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = 3.54f,
                dy1 = 1.46f,
            )
            arcTo(
                horizontalEllipseRadius = 4.8f,
                verticalEllipseRadius = 4.8f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 21.0f,
                y1 = 15.0f,
            )
            verticalLineToRelative(dy = 4.0f)
            close()
        }
    }
}

@Preview(name = "OutlinedReply", showBackground = true)
@Composable
private fun OutlinedReplyPreview() {
    Icon(imageVector = OutlinedReply)
}
