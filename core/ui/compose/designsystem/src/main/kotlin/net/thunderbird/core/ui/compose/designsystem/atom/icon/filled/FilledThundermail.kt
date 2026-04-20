package net.thunderbird.core.ui.compose.designsystem.atom.icon.filled

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.icon
import net.thunderbird.core.ui.compose.designsystem.atom.icon.iconPath

internal val FilledThundermail: ImageVector by lazy {
    icon(name = "FilledThundermail", viewportWidth = 18f, viewportHeight = 18f) {
        iconPath(
            fill = SolidColor(Color.White),
            fillAlpha = 0.2f,
            strokeAlpha = 0.2f,
        ) {
            moveTo(x = 12.98f, y = 8.11f)
            lineToRelative(dx = -2.2f, dy = 4.8f)
            lineToRelative(dx = -0.38f, dy = -0.18f)
            lineToRelative(dx = -0.38f, dy = -0.18f)
            lineToRelative(dx = 2.2f, dy = -4.79f)
            close()
            moveTo(x = 8.25f, y = 2.63f)
            lineTo(x = 5.43f, y = 9.05f)
            lineTo(x = 4.66f, y = 8.71f)
            lineTo(x = 7.48f, y = 2.3f)
            close()
            moveToRelative(dx = 1.92f, dy = 0.0f)
            lineTo(x = 7.36f, y = 9.05f)
            lineTo(x = 6.58f, y = 8.71f)
            lineTo(x = 9.4f, y = 2.3f)
            close()
        }
        iconPath(
            fill = SolidColor(Color.White),
        ) {
            moveTo(x = 11.62f, y = 1.62f)
            arcToRelative(
                a = 1.3f,
                b = 1.3f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = 1.2f,
                dy1 = 1.8f,
            )
            lineTo(x = 12.8f, y = 3.46f)
            lineTo(x = 11.3f, y = 6.7f)
            lineToRelative(dx = -2.64f, dy = 5.76f)
            lineToRelative(dx = -0.88f, dy = 1.92f)
            lineToRelative(dx = 6.1f, dy = -5.3f)
            horizontalLineToRelative(dx = -2.2f)
            lineToRelative(dx = 0.59f, dy = -1.27f)
            horizontalLineToRelative(dx = 3.3f)
            arcTo(
                horizontalEllipseRadius = 0.63f,
                verticalEllipseRadius = 0.63f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 16.0f,
                y1 = 8.9f,
            )
            lineToRelative(dx = -8.43f, dy = 7.34f)
            curveToRelative(dx1 = -0.78f, dy1 = 0.68f, dx2 = -1.94f, dy2 = -0.19f, dx3 = -1.5f, dy3 = -1.13f)
            lineTo(x = 8.4f, y = 9.99f)
            lineToRelative(dx = 1.75f, dy = -3.82f)
            lineToRelative(dx = 1.49f, dy = -3.24f)
            verticalLineTo(y = 2.9f)
            lineToRelative(dx = -0.02f, dy = -0.01f)
            horizontalLineTo(x = 7.02f)
            arcToRelative(
                a = 1.0f,
                b = 1.0f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                dx1 = -0.94f,
                dy1 = 0.6f,
            )
            lineTo(x = 6.07f, y = 3.53f)
            lineTo(x = 4.32f, y = 7.8f)
            horizontalLineToRelative(dx = 3.63f)
            lineTo(x = 7.37f, y = 9.07f)
            horizontalLineToRelative(dx = -4.0f)
            arcTo(
                horizontalEllipseRadius = 0.63f,
                verticalEllipseRadius = 0.63f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 2.8f,
                y1 = 8.2f,
            )
            lineToRelative(dx = 2.1f, dy = -5.15f)
            lineToRelative(dx = 0.04f, dy = -0.08f)
            arcToRelative(
                a = 2.3f,
                b = 2.3f,
                theta = 0.0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                dx1 = 2.1f,
                dy1 = -1.35f,
            )
            close()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilledThundermailPreview() {
    Icon(imageVector = FilledThundermail)
}
